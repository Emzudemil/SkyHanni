package at.hannibal2.skyhanni.data.model

enum class ComposterUpgrade(val displayName: String, val slotNumber: Int) {
    COMPOSTER_SPEED("Composter Speed", 20),
    MULTI_DROP("Multi Drop", 21),
    FUEL_CAP("Fuel Cap", 22),
    ORGANIC_MATTER_CAP("Organic Matter Cap", 23),
    COST_REDUCTION("Cost Reduction", 24),
    ;

    companion object {
        private fun regexValues() = values().joinToString("|") { it.displayName }

        val regex = "§a(?<name>${regexValues()})(?: (?<level>.*))?".toPattern()

        fun getByName(name: String) = values().firstOrNull { it.displayName == name }
    }
}