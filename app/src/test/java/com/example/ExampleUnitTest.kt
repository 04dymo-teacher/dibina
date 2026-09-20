package com.example

import com.example.dibina.domain.BadgeSystem
import com.example.dibina.domain.ExpCalculator
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testLevelCalculation() {
        assertEquals(1, ExpCalculator.calculateLevel(0L))
        assertEquals(1, ExpCalculator.calculateLevel(1999L))
        assertEquals(2, ExpCalculator.calculateLevel(2000L))
        assertEquals(2, ExpCalculator.calculateLevel(3999L))
        assertEquals(3, ExpCalculator.calculateLevel(4000L))
        assertEquals(13, ExpCalculator.calculateLevel(24000L))
    }

    @Test
    fun testDashboardHeaderColorsCycleEveryMultipleOf3Levels() {
        val level1Colors = ExpCalculator.getDashboardHeaderColors(1)
        val level2Colors = ExpCalculator.getDashboardHeaderColors(2)
        val level3Colors = ExpCalculator.getDashboardHeaderColors(3)
        val level6Colors = ExpCalculator.getDashboardHeaderColors(6)
        val level9Colors = ExpCalculator.getDashboardHeaderColors(9)

        // Level 1 and 2 are in cycle 0
        assertEquals(level1Colors, level2Colors)

        // Level 3 shifts to the next blue-white identity theme
        assertNotEquals(level1Colors.primary, level3Colors.primary)

        // Level 6 shifts to cycle 2
        assertNotEquals(level3Colors.primary, level6Colors.primary)

        // Level 9 shifts to cycle 3
        assertNotEquals(level6Colors.primary, level9Colors.primary)
    }

    @Test
    fun testBadgeHierarchy13Tiers() {
        assertEquals(13, BadgeSystem.ALL_BADGES.size)

        // Verify the exact names specified
        val expectedBadgeNames = listOf(
            "Recruit",
            "Private",
            "Corporal",
            "Sergeant",
            "Veteran",
            "Elite",
            "Captain",
            "Commander",
            "Warlord",
            "Champion",
            "Legendary",
            "Mythic",
            "Immortal"
        )

        val actualBadgeNames = BadgeSystem.ALL_BADGES.map { it.name }
        assertEquals(expectedBadgeNames, actualBadgeNames)

        // Recruit is unlocked at 0 EXP Level 1
        val recruit = BadgeSystem.getActiveBadge(0L, 1)
        assertEquals("Recruit", recruit.name)

        // Corporal at 4000 EXP Level 3
        val corporal = BadgeSystem.getActiveBadge(4000L, 3)
        assertEquals("Corporal", corporal.name)

        // Immortal at 24000 EXP Level 13
        val immortal = BadgeSystem.getActiveBadge(24000L, 13)
        assertEquals("Immortal", immortal.name)
    }

    @Test
    fun testLockedBadgesVisibility() {
        // For a new student (Level 1, 0 EXP):
        val allBadges = BadgeSystem.ALL_BADGES
        val unlocked = allBadges.filter { BadgeSystem.isBadgeUnlocked(it, 0L, 1) }
        val locked = allBadges.filter { !BadgeSystem.isBadgeUnlocked(it, 0L, 1) }

        assertEquals(1, unlocked.size) // Only Recruit
        assertEquals(12, locked.size)  // 12 locked badges remain visible as locked
    }
}
