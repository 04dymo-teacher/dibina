const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

admin.initializeApp();
const db = admin.firestore();

/**
 * 13 Official Badges Hierarchy
 */
const BADGES = [
  { id: "recruit", name: "Recruit", nameId: "Rekrut", minExp: 0, minLevel: 1, icon: "🛡️" },
  { id: "private", name: "Private", nameId: "Prajurit", minExp: 2000, minLevel: 2, icon: "⚔️" },
  { id: "corporal", name: "Corporal", nameId: "Kopral", minExp: 4000, minLevel: 3, icon: "🎖️" },
  { id: "sergeant", name: "Sergeant", nameId: "Sersan", minExp: 6000, minLevel: 4, icon: "🏅" },
  { id: "veteran", name: "Veteran", nameId: "Veteran", minExp: 8000, minLevel: 5, icon: "🎗️" },
  { id: "elite", name: "Elite", nameId: "Elit", minExp: 10000, minLevel: 6, icon: "🌟" },
  { id: "captain", name: "Captain", nameId: "Kapten", minExp: 12000, minLevel: 7, icon: "⚡" },
  { id: "commander", name: "Commander", nameId: "Komandan", minExp: 14000, minLevel: 8, icon: "🦅" },
  { id: "warlord", name: "Warlord", nameId: "Panglima", minExp: 16000, minLevel: 9, icon: "👑" },
  { id: "champion", name: "Champion", nameId: "Kampiun", minExp: 18000, minLevel: 10, icon: "🏆" },
  { id: "legendary", name: "Legendary", nameId: "Legendaris", minExp: 20000, minLevel: 11, icon: "🔮" },
  { id: "mythic", name: "Mythic", nameId: "Mitis", minExp: 22000, minLevel: 12, icon: "🪐" },
  { id: "immortal", name: "Immortal", nameId: "Abadi", minExp: 24000, minLevel: 13, icon: "💎" }
];

/**
 * Cloud Function: joinClassByCode
 * Resolves classCode -> classId on server.
 * Do not trust classId from the client.
 */
exports.joinClassByCode = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Siswa harus masuk terlebih dahulu.");
  }

  const { classCode, studentName, profilePhotoUrl } = data;
  if (!classCode || typeof classCode !== "string") {
    throw new functions.https.HttpsError("invalid-argument", "Kode kelas wajib diisi.");
  }

  const trimmedCode = classCode.trim().toUpperCase();
  const classSnapshot = await db.collection("classes")
    .where("classCode", "==", trimmedCode)
    .limit(1)
    .get();

  if (classSnapshot.empty) {
    throw new functions.https.HttpsError("not-found", `Kode kelas '${trimmedCode}' tidak ditemukan.`);
  }

  const classDoc = classSnapshot.docs[0];
  const classId = classDoc.id;
  const classData = classDoc.data();
  const uid = context.auth.uid;
  const now = Date.now();

  const batch = db.batch();

  // 1. Update users/{uid}
  const userRef = db.collection("users").document(uid);
  batch.set(userRef, {
    classId: classId,
    classCode: trimmedCode,
    updatedAt: now
  }, { merge: true });

  // 2. Add to classes/{classId}/members/{uid}
  const memberRef = db.collection("classes").document(classId)
    .collection("members").document(uid);
  batch.set(memberRef, {
    uid: uid,
    classId: classId,
    classCode: trimmedCode,
    studentName: studentName || "Siswa DIBINA",
    profilePhotoUrl: profilePhotoUrl || null,
    role: "student",
    joinedAt: now
  }, { merge: true });

  // 3. Increment studentCount in class document
  batch.update(classDoc.ref, {
    studentCount: admin.firestore.FieldValue.increment(1),
    updatedAt: now
  });

  // 4. Record auditLog
  const auditRef = db.collection("auditLogs").doc();
  batch.set(auditRef, {
    logId: auditRef.id,
    actorUid: uid,
    action: "JOIN_CLASS",
    entityType: "CLASS",
    entityId: classId,
    details: { classCode: trimmedCode },
    timestamp: now
  });

  await batch.commit();

  return {
    classId: classId,
    classCode: trimmedCode,
    className: classData.className || `Kelas ${trimmedCode}`
  };
});

/**
 * Helper: Parse time string (HH.MM or HH:MM) to decimal hours
 */
function parseTimeToDecimal(timeStr) {
  if (!timeStr || typeof timeStr !== "string") return -1;
  const parts = timeStr.trim().replace(":", ".").split(".");
  if (parts.length < 2) return -1;
  const hours = parseInt(parts[0], 10);
  const minutes = parseInt(parts[1], 10);
  if (isNaN(hours) || isNaN(minutes)) return -1;
  return hours + (minutes / 60.0);
}

/**
 * Helper: Parse YYYYMMDD to Date
 */
function parseDateString(dateStr) {
  if (!dateStr || dateStr.length !== 8) return null;
  const y = parseInt(dateStr.substring(0, 4), 10);
  const m = parseInt(dateStr.substring(4, 6), 10) - 1;
  const d = parseInt(dateStr.substring(6, 8), 10);
  return new Date(y, m, d);
}

/**
 * Firestore Trigger: onJournalWritten
 *
 * ALL FINAL EXP CALCULATIONS ARE SERVER-SIDE.
 * The Android client must never be trusted for:
 * exp, totalExp, level, streak, achievement.
 *
 * EXP Rules:
 * - Max 10 EXP per habit, normal max 70 EXP.
 * 1. Bangun Pagi: 04.00-06.00 = 10 EXP, Outside = 0 EXP.
 * 2. Beribadah: 5 = 10 EXP, 4 = 8 EXP, 3 = 6 EXP, 2 = 4 EXP, 1 = 2 EXP, 0 = 0 EXP.
 * 3. Berolahraga: done = 10 EXP.
 * 4. Makan Sehat:
 *    Karbohidrat, Protein, Lemak, Vitamin, Serat, Air (NO MINERAL).
 *    >4 categories = 10 EXP, >3 = 8 EXP, >2 = 6 EXP, >1 = 4 EXP, <=1 = 0 EXP.
 * 5. Gemar Belajar: materi + durasi (multiples of 15 min, >=15) = 10 EXP.
 * 6. Bermasyarakat: done = 10 EXP.
 * 7. Tidur Cepat: done (19.30-21.30 or valid time) = 10 EXP.
 *
 * Backdate:
 * - Max 7 days in past.
 * - Backdated journal: Max 5 EXP per habit.
 * - Backdated journal does NOT increase streak!
 *
 * Level:
 * - Default: Level 1, EXP 0.
 * - Every 2,000 EXP = +1 level (e.g. 0-1,999 = Level 1, 2,000-3,999 = Level 2).
 *
 * Streak:
 * - Real-time only. Missing one day breaks streak.
 */
exports.onJournalWritten = functions.firestore
  .document("journals/{journalId}")
  .onWrite(async (change, context) => {
    const { journalId } = context.params;

    if (!change.after.exists) {
      return null;
    }

    const journal = change.after.data();
    const isNew = !change.before.exists;
    const now = Date.now();

    // 1. Validate deterministic ID format: uid_YYYYMMDD
    const expectedId = `${journal.uid}_${journal.date}`;
    if (journalId !== expectedId) {
      console.error(`Invalid deterministic journal ID: ${journalId}, expected: ${expectedId}`);
      await change.after.ref.update({
        error: "INVALID_DETERMINISTIC_ID"
      });
      return null;
    }

    // 2. Enforce constraint: strictly NO mineral field!
    if ("mineral" in journal) {
      await change.after.ref.update({
        mineral: admin.firestore.FieldValue.delete()
      });
    }

    // Determine backdate server-side: compare journal.date with current date in Asia/Jakarta (WIB)
    const journalDate = parseDateString(journal.date);
    const todayWibStr = new Intl.DateTimeFormat("en-CA", {
      timeZone: "Asia/Jakarta",
      year: "numeric",
      month: "2-digit",
      day: "2-digit"
    }).format(new Date()).replace(/-/g, "");

    const todayDate = parseDateString(todayWibStr);
    let diffDays = 0;
    if (journalDate && todayDate) {
      const diffTime = todayDate.getTime() - journalDate.getTime();
      diffDays = Math.round(diffTime / (1000 * 3600 * 24));
    }

    const isBackdate = diffDays > 0;
    const isFuture = diffDays < 0;

    if (isFuture || diffDays > 7) {
      console.error(`Invalid journal date: ${journal.date} (diffDays: ${diffDays})`);
      await change.after.ref.update({
        error: isFuture ? "FUTURE_DATE_NOT_ALLOWED" : "EXCEEDS_7_DAYS_BACKDATE"
      });
      return null;
    }

    // 3. Calculate EXP per habit server-side
    // 1. Bangun Pagi: 04.00–06.00 = 10 EXP, outside = 0 EXP
    let expBangunPagi = 0;
    const wakeTime = parseTimeToDecimal(journal.bangunPagiTime);
    if (wakeTime >= 4.0 && wakeTime <= 6.0) {
      expBangunPagi = 10;
    }

    // 2. Beribadah: 5 = 10 EXP, 4 = 8 EXP, 3 = 6 EXP, 2 = 4 EXP, 1 = 2 EXP, 0 = 0 EXP
    let worshipCount = 0;
    if (Array.isArray(journal.ibadahSholat)) {
      worshipCount = journal.ibadahSholat.length;
    } else if (journal.ibadahDetails && journal.ibadahDetails.trim().length > 0) {
      const parts = journal.ibadahDetails.split(",").filter(s => s.trim().length > 0);
      worshipCount = Math.min(5, Math.max(1, parts.length));
    }
    const worshipExpMap = [0, 2, 4, 6, 8, 10];
    let expIbadah = worshipExpMap[Math.min(5, Math.max(0, worshipCount))];

    // 3. Berolahraga: 10 EXP if completed
    let expOlahraga = 0;
    if (journal.olahraga || (journal.olahragaActivity && journal.olahragaActivity.trim().length > 0)) {
      expOlahraga = 10;
    }

    // 4. Makan Sehat: Valid categories: Karbohidrat, Protein, Lemak, Vitamin, Serat, Air (NO MINERAL)
    // >4 = 10 EXP, >3 = 8 EXP, >2 = 6 EXP, >1 = 4 EXP, <=1 = 0 EXP
    const nutritionCategoriesCount = [
      journal.karbohidrat || (journal.karbohidratText && journal.karbohidratText.trim().length > 0),
      journal.protein || (journal.proteinText && journal.proteinText.trim().length > 0),
      journal.lemak || (journal.lemakText && journal.lemakText.trim().length > 0),
      journal.vitamin || (journal.vitaminText && journal.vitaminText.trim().length > 0),
      journal.serat || (journal.seratText && journal.seratText.trim().length > 0),
      journal.air || (journal.airGelas && journal.airGelas >= 1)
    ].filter(Boolean).length;

    let expMakanSehat = 0;
    if (nutritionCategoriesCount > 4) { // 5 or 6 categories
      expMakanSehat = 10;
    } else if (nutritionCategoriesCount > 3) { // 4 categories
      expMakanSehat = 8;
    } else if (nutritionCategoriesCount > 2) { // 3 categories
      expMakanSehat = 6;
    } else if (nutritionCategoriesCount > 1) { // 2 categories
      expMakanSehat = 4;
    } else {
      expMakanSehat = 0;
    }

    // 5. Gemar Belajar: materiBelajar + durasiBelajar (must be multiple of 15 min, >= 15 min)
    let expBelajar = 0;
    const durasi = journal.durasiBelajar || 0;
    const hasMateri = journal.materiBelajar && journal.materiBelajar.trim().length > 0;
    if (hasMateri && durasi >= 15 && durasi % 15 === 0) {
      expBelajar = 10;
    }

    // 6. Bermasyarakat: 10 EXP if completed
    let expBermasyarakat = 0;
    if (journal.bermasyarakat || (journal.bermasyarakatActivity && journal.bermasyarakatActivity.trim().length > 0)) {
      expBermasyarakat = 10;
    }

    // 7. Tidur Cepat: 10 EXP if completed
    let expTidur = 0;
    if (journal.tidurCepat || (journal.tidurCepatTime && journal.tidurCepatTime.trim().length > 0)) {
      expTidur = 10;
    }

    // Apply backdate rule: Maximum 5 EXP per habit
    if (isBackdate) {
      expBangunPagi = Math.min(5, expBangunPagi);
      expIbadah = Math.min(5, expIbadah);
      expOlahraga = Math.min(5, expOlahraga);
      expMakanSehat = Math.min(5, expMakanSehat);
      expBelajar = Math.min(5, expBelajar);
      expBermasyarakat = Math.min(5, expBermasyarakat);
      expTidur = Math.min(5, expTidur);
    }

    const calculatedExp = expBangunPagi + expIbadah + expOlahraga + expMakanSehat + expBelajar + expBermasyarakat + expTidur;
    const habitsCompletedCount = [
      expBangunPagi > 0,
      expIbadah > 0,
      expOlahraga > 0,
      expMakanSehat > 0,
      expBelajar > 0,
      expBermasyarakat > 0,
      expTidur > 0
    ].filter(Boolean).length;

    const userRef = db.collection("users").document(journal.uid);
    const deterministicStatId = `${journal.uid}_${journal.date}`;

    // Update journal doc with the definitive server-calculated EXP
    await change.after.ref.update({
      exp: calculatedExp,
      isBackdate: isBackdate,
      habitsCompletedCount: habitsCompletedCount
    });

    // 4. Update dailyStats/{uid_YYYYMMDD}
    const statRef = db.collection("dailyStats").document(deterministicStatId);
    await statRef.set({
      statId: deterministicStatId,
      uid: journal.uid,
      classId: journal.classId || "",
      date: journal.date,
      habitsCompleted: habitsCompletedCount,
      totalHabits: 7,
      expEarned: calculatedExp,
      isBackdate: isBackdate,
      createdAt: journal.createdAt || now,
      updatedAt: now
    }, { merge: true });

    // 5. Update feedPosts/{postId}
    const feedRef = db.collection("feedPosts").document(journalId);
    const userDoc = await userRef.get();
    const userData = userDoc.exists ? userDoc.data() : {};

    await feedRef.set({
      postId: journalId,
      journalId: journalId,
      uid: journal.uid,
      classId: journal.classId || "",
      studentName: userData.name || "Siswa DIBINA",
      profilePhotoUrl: userData.profilePhotoUrl || null,
      date: journal.date,
      summary: `Menyelesaikan ${habitsCompletedCount} dari 7 Kebiasaan Anak Indonesia Hebat.`,
      materiBelajar: journal.materiBelajar || null,
      cheerCount: 0,
      createdAt: journal.createdAt || now
    }, { merge: true });

    // 6. Update user stats in transaction: EXP, Level, Streak, and Badges
    await db.runTransaction(async (transaction) => {
      const uDoc = await transaction.get(userRef);
      if (!uDoc.exists) return;

      const currentTotalExp = uDoc.data().totalExp || 0;
      const prevJournalExp = isNew ? 0 : (change.before.data().exp || 0);
      const expDiff = calculatedExp - prevJournalExp;

      const newTotalExp = Math.max(0, currentTotalExp + expDiff);

      // Level: Every 2,000 EXP = +1 level (0–1,999 = Level 1, 2,000–3,999 = Level 2, etc.)
      const newLevel = 1 + Math.floor(newTotalExp / 2000);

      // Streak calculation
      let currentStreak = uDoc.data().currentStreak || 0;
      let longestStreak = uDoc.data().longestStreak || 0;
      const lastRealtimeJournalDate = uDoc.data().lastRealtimeJournalDate || null;

      if (!isBackdate) {
        if (!lastRealtimeJournalDate) {
          currentStreak = 1;
        } else if (lastRealtimeJournalDate === journal.date) {
          // Same day edit -> streak unchanged
        } else {
          // Check if lastRealtimeJournalDate was yesterday
          const lastDate = parseDateString(lastRealtimeJournalDate);
          let dayGap = 999;
          if (lastDate && journalDate) {
            dayGap = Math.round((journalDate.getTime() - lastDate.getTime()) / (1000 * 3600 * 24));
          }

          if (dayGap === 1) {
            currentStreak += 1;
          } else if (dayGap > 1) {
            // Missing one real-time day breaks streak
            currentStreak = 1;
          }
        }
      }
      // Note: Backdated journal does NOT increase streak!

      longestStreak = Math.max(longestStreak, currentStreak);

      // Determine highest unlocked badge
      const unlockedBadges = BADGES.filter(b => newTotalExp >= b.minExp && newLevel >= b.minLevel);
      const activeBadge = unlockedBadges[unlockedBadges.length - 1] || BADGES[0];

      const updateData = {
        totalExp: newTotalExp,
        level: newLevel,
        longestStreak: longestStreak,
        badgeId: activeBadge.id,
        badgeName: activeBadge.name,
        updatedAt: now
      };

      if (!isBackdate) {
        updateData.currentStreak = currentStreak;
        updateData.lastRealtimeJournalDate = journal.date;
      }

      transaction.update(userRef, updateData);

      // Also update achievements/{uid} document
      const achievementRef = db.collection("achievements").document(journal.uid);
      transaction.set(achievementRef, {
        uid: journal.uid,
        totalExp: newTotalExp,
        level: newLevel,
        currentStreak: currentStreak,
        longestStreak: longestStreak,
        activeBadgeId: activeBadge.id,
        activeBadgeName: activeBadge.name,
        unlockedBadgeIds: unlockedBadges.map(b => b.id),
        totalBadgesCount: unlockedBadges.length,
        updatedAt: now
      }, { merge: true });
    });

    // 7. Synchronize to Google Apps Script Webhook
    const gasWebhookUrl = process.env.GAS_WEBHOOK_URL || "TODO: MANUAL CONFIGURATION REQUIRED";
    if (gasWebhookUrl && gasWebhookUrl.startsWith("http")) {
      try {
        const payload = {
          journalId: journalId,
          uid: journal.uid,
          studentName: userData.name || "Siswa",
          classCode: userData.classCode || journal.classId,
          date: journal.date,
          bangunPagi: expBangunPagi > 0,
          ibadah: expIbadah > 0,
          olahraga: expOlahraga > 0,
          makanSehat: expMakanSehat > 0,
          materiBelajar: journal.materiBelajar || "",
          durasiBelajar: journal.durasiBelajar || 0,
          bermasyarakat: expBermasyarakat > 0,
          tidurCepat: expTidur > 0,
          totalHabitsCompleted: habitsCompletedCount,
          expEarned: calculatedExp,
          isBackdate: isBackdate,
          timestamp: new Date().toISOString()
        };

        await axios.post(gasWebhookUrl, payload, { timeout: 8000 });
        await change.after.ref.update({
          spreadsheetSyncStatus: "SYNCED",
          spreadsheetSyncedAt: now,
          spreadsheetSyncError: null
        });
      } catch (gasError) {
        console.warn("GAS Webhook Sync Error:", gasError.message);
        await change.after.ref.update({
          spreadsheetSyncStatus: "ERROR",
          spreadsheetSyncError: gasError.message
        });
      }
    }

    // 8. Record audit log
    await db.collection("auditLogs").add({
      logId: `${journalId}_log`,
      actorUid: journal.uid,
      action: isNew ? "CREATE_JOURNAL" : "UPDATE_JOURNAL",
      entityType: "JOURNAL",
      entityId: journalId,
      details: {
        habitsCompletedCount: habitsCompletedCount,
        exp: calculatedExp,
        isBackdate: isBackdate
      },
      timestamp: now
    });

    return null;
  });

/**
 * Scheduled Cloud Function: sendDailyHabitReminders
 * Runs twice daily (WIB):
 * - 05.00 WIB: Morning reminder (Bangun Pagi & Ibadah)
 * - 20.30 WIB: Evening reminder (Review jurnal 7 KAIH & Tidur Cepat)
 */
exports.sendDailyHabitReminders = functions.pubsub
  .schedule("0 5,20 * * *")
  .timeZone("Asia/Jakarta")
  .onRun(async (context) => {
    const hour = new Date().getHours();
    const isMorning = hour < 12;

    const title = isMorning
      ? "Selamat Pagi Juara DIBINA! ☀️"
      : "Saatnya Beristirahat & Catat KAIH 🌙";

    const body = isMorning
      ? "Ayo awali harimu dengan Bangun Pagi dan Beribadah tepat waktu!"
      : "Yuk periksa jurnal harianmu dan pastikan tidur cepat sebelum 21.30.";

    const usersSnapshot = await db.collection("users")
      .where("role", "==", "student")
      .limit(500)
      .get();

    const tokens = [];
    usersSnapshot.forEach(doc => {
      const data = doc.data();
      if (data.fcmToken) {
        tokens.push(data.fcmToken);
      }
    });

    if (tokens.length === 0) {
      return null;
    }

    const payload = {
      notification: { title, body },
      data: {
        type: "DAILY_REMINDER",
        time: isMorning ? "MORNING" : "EVENING"
      }
    };

    try {
      await admin.messaging().sendEachForMulticast({
        tokens: tokens,
        notification: payload.notification,
        data: payload.data
      });
    } catch (e) {
      console.error("FCM broadcast error:", e);
    }

    return null;
  });
