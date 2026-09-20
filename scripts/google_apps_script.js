/**
 * Google Apps Script for DIBINA Teacher Reporting
 * Destination: Google Spreadsheet
 *
 * Deployment Instructions:
 * 1. Open Google Sheets -> Extensions -> Apps Script.
 * 2. Paste this code.
 * 3. Deploy as Web App (Execute as: Me, Who has access: Anyone).
 * 4. Copy the Web App URL into Firebase Cloud Functions environment:
 *    GOOGLE_APPS_SCRIPT_WEBHOOK_URL
 *
 * TODO: MANUAL CONFIGURATION REQUIRED
 * - Provide target Google Spreadsheet ID or bind script directly to the sheet container.
 */

function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();

    // Initialize Header Row if empty
    if (sheet.getLastRow() === 0) {
      sheet.appendRow([
        "Timestamp",
        "Tanggal Jurnal",
        "Nama Siswa",
        "UID Siswa",
        "Kode Kelas",
        "1. Bangun Pagi",
        "2. Beribadah",
        "3. Berolahraga",
        "4. Makan Sehat & Bergizi",
        "5. Gemar Membaca",
        "6. Bermasyarakat / Tolong Menolong",
        "7. Tidur Tepat Waktu",
        "Total Kebiasaan Terpenuhi (/7)",
        "EXP Didapat",
        "Catatan Refleksi Siswa",
        "Tipe Event"
      ]);
      sheet.getRange(1, 1, 1, 16).setFontWeight("bold").setBackground("#E1F5FE");
    }

    var habits = data.habits || {};
    var row = [
      data.timestamp || new Date().toISOString(),
      data.date || "",
      data.studentName || "",
      data.uid || "",
      data.classId || "",
      habits["bangun_pagi"] && habits["bangun_pagi"].completed ? "YA" : "TIDAK",
      habits["beribadah"] && habits["beribadah"].completed ? "YA" : "TIDAK",
      habits["berolahraga"] && habits["berolahraga"].completed ? "YA" : "TIDAK",
      habits["makan_sehat"] && habits["makan_sehat"].completed ? "YA" : "TIDAK",
      habits["gemar_membaca"] && habits["gemar_membaca"].completed ? "YA" : "TIDAK",
      habits["bermasyarakat"] && habits["bermasyarakat"].completed ? "YA" : "TIDAK",
      habits["tidur_tepat_waktu"] && habits["tidur_tepat_waktu"].completed ? "YA" : "TIDAK",
      data.completedCount || 0,
      data.expEarned || 0,
      data.reflection || "",
      data.eventType || "JOURNAL_SUBMISSION"
    ];

    sheet.appendRow(row);

    return ContentService.createTextOutput(JSON.stringify({ status: "success", message: "Journal appended successfully" }))
      .setMimeType(ContentService.MimeType.JSON);

  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({ status: "error", message: error.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}
