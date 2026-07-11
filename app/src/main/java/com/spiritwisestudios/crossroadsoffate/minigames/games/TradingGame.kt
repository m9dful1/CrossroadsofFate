package com.spiritwisestudios.crossroadsoffate.minigames.games

import com.spiritwisestudios.crossroadsoffate.minigames.*
import kotlin.random.Random

/**
 * Trading negotiation mini-game where players negotiate with NPCs for better deals.
 * Success depends on reading NPC mood and making appropriate choices.
 */
class TradingGame(
    private val itemValue: Int = 100,
    private val npcPersonality: NPCPersonality = NPCPersonality.BALANCED
) : MiniGame() {
    
    override val id: String = "trading_${npcPersonality.name.lowercase()}"
    override val name: String = "Trading Negotiation"
    override val difficulty: Int = when(npcPersonality) {
        NPCPersonality.GREEDY -> 4
        NPCPersonality.STUBBORN -> 3
        NPCPersonality.FRIENDLY -> 2
        NPCPersonality.BALANCED -> 3
    }
    override val description: String = "Negotiate with a ${npcPersonality.name.lowercase()} merchant to get the best deal"
    override val instructions: String = """
        Negotiate wisely to get the best price!
        
        • Watch the merchant's mood and reactions
        • Choose your negotiation strategy carefully
        • Push too hard and they might walk away
        • Be too passive and you'll pay full price
        • Different merchants respond to different approaches
    """.trimIndent()
    
    enum class NPCPersonality {
        GREEDY,     // Wants high prices, responds to persistence
        FRIENDLY,   // Easy to negotiate with, likes politeness
        STUBBORN,   // Hard to move, needs convincing arguments
        BALANCED    // Moderate responses to all approaches
    }
    
    enum class NPCMood {
        HAPPY, NEUTRAL, ANNOYED, ANGRY
    }
    
    enum class NegotiationApproach {
        POLITE_REQUEST,     // "Could you perhaps lower the price a bit?"
        FIRM_COUNTER,       // "I can offer you X coins for this"
        WALK_AWAY_THREAT,   // "I'll take my business elsewhere"
        COMPLIMENT_FLATTERY, // "A merchant of your reputation surely..."
        POINT_OUT_FLAWS     // "This item has some issues though..."
    }
    
    companion object {
        private const val MAX_ROUNDS = 6
        private const val MOOD_THRESHOLD_ANGRY = -3
        private const val STARTING_PRICE_MULTIPLIER = 1.5f

        /**
         * Negotiation choices shown to the player: input key to display label.
         * Shared with the UI so the choice list has a single source of truth.
         */
        val AVAILABLE_CHOICES = listOf(
            "polite" to "Ask politely for a lower price",
            "firm" to "Make a firm counteroffer",
            "threat" to "Threaten to walk away",
            "compliment" to "Compliment the merchant",
            "flaws" to "Point out flaws in the item"
        )
    }
    
    override fun initialize(): MiniGameState {
        val startingPrice = (itemValue * STARTING_PRICE_MULTIPLIER).toInt()
        val targetPrice = (itemValue * 0.8f).toInt() // Player target for good deal
        
        return MiniGameState(
            isActive = true,
            maxAttempts = MAX_ROUNDS,
            currentData = mapOf<String, Any>(
                "currentPrice" to startingPrice,
                "originalPrice" to startingPrice,
                "targetPrice" to targetPrice,
                "itemValue" to itemValue,
                "npcMood" to NPCMood.NEUTRAL,
                "moodScore" to 0,
                "round" to 1,
                "negotiationHistory" to listOf<String>(),
                "dealClosed" to false
            )
        )
    }
    
    override fun processGameInput(currentState: MiniGameState, input: MiniGameInput): MiniGameState {
        return when (input) {
            is MiniGameInput.Choice -> processNegotiation(currentState, input.choice)
            is MiniGameInput.Confirm -> acceptDeal(currentState)
            else -> currentState
        }
    }
    
    private fun processNegotiation(state: MiniGameState, choice: String): MiniGameState {
        val currentRound = state.getData<Int>("round") ?: 1
        if (currentRound > MAX_ROUNDS) {
            return state.copy(isCompleted = true)
        }

        val approach = when (choice) {
            "polite" -> NegotiationApproach.POLITE_REQUEST
            "firm" -> NegotiationApproach.FIRM_COUNTER
            "threat" -> NegotiationApproach.WALK_AWAY_THREAT
            "compliment" -> NegotiationApproach.COMPLIMENT_FLATTERY
            "flaws" -> NegotiationApproach.POINT_OUT_FLAWS
            else -> return state
        }
        
        val currentPrice = state.getData<Int>("currentPrice") ?: return state
        val moodScore = state.getData<Int>("moodScore") ?: 0
        val round = state.getData<Int>("round") ?: 1
        val history = state.getData<List<String>>("negotiationHistory") ?: emptyList()
        
        // Calculate NPC response based on personality and approach
        val response = calculateNPCResponse(approach, moodScore, round)
        val newMoodScore = moodScore + response.moodChange
        val newPrice = maxOf(itemValue / 2, currentPrice + response.priceChange)
        val newMood = calculateMood(newMoodScore)
        
        // Add to history
        val newHistory = history + formatNegotiationRound(approach, response, newPrice)
        
        // Check if NPC walks away
        if (newMoodScore <= MOOD_THRESHOLD_ANGRY) {
            return state.copy(
                isCompleted = true,
                currentData = state.currentData + mapOf<String, Any>(
                    "dealClosed" to false,
                    "walkAway" to true,
                    "finalMessage" to "The merchant storms off, refusing to deal with you!"
                )
            )
        }
        
        return state.copy(
            attempts = state.attempts + 1,
            currentData = state.currentData + mapOf<String, Any>(
                "currentPrice" to newPrice,
                "moodScore" to newMoodScore,
                "npcMood" to newMood,
                "round" to round + 1,
                "lastApproach" to approach,
                "negotiationHistory" to newHistory,
                "lastResponse" to response.message
            )
        )
    }
    
    private fun acceptDeal(state: MiniGameState): MiniGameState {
        val currentPrice = state.getData<Int>("currentPrice") ?: return state
        val originalPrice = state.getData<Int>("originalPrice") ?: return state
        
        return state.copy(
            isCompleted = true,
            currentData = state.currentData + mapOf<String, Any>(
                "dealClosed" to true,
                "finalPrice" to currentPrice,
                "savings" to (originalPrice - currentPrice)
            )
        )
    }
    
    override fun checkCompletion(state: MiniGameState): MiniGameResult? {
        val dealClosed = state.getData<Boolean>("dealClosed") ?: false
        val walkAway = state.getData<Boolean>("walkAway") ?: false
        val round = state.getData<Int>("round") ?: 1
        
        // Deal completed successfully
        if (dealClosed && !walkAway) {
            val finalPrice = state.getData<Int>("finalPrice") ?: return null
            val originalPrice = state.getData<Int>("originalPrice") ?: return null
            val targetPrice = state.getData<Int>("targetPrice") ?: return null
            val savings = originalPrice - finalPrice
            val savingsPercent = (savings.toFloat() / originalPrice) * 100
            
            val success = finalPrice <= targetPrice
            val score = calculateFinalScore(savings, originalPrice, state.attempts)
            
            return MiniGameResult(
                isCompleted = true,
                success = success,
                score = score,
                finalProgress = 1.0f,
                rewards = getRewards(success, savingsPercent),
                message = when {
                    savingsPercent >= 40 -> "Excellent negotiation! You got an amazing deal!"
                    savingsPercent >= 25 -> "Good work! You saved quite a bit."
                    savingsPercent >= 10 -> "Not bad. You managed to save some coins."
                    else -> "You paid close to full price, but the deal is done."
                }
            )
        }
        
        // NPC walked away
        if (walkAway) {
            return MiniGameResult(
                isCompleted = true,
                success = false,
                score = 0,
                finalProgress = 0f,
                consequences = listOf("reputation_loss"),
                message = state.getData<String>("finalMessage") ?: "Negotiation failed!"
            )
        }
        
        // Reached maximum rounds without closing
        if (round > MAX_ROUNDS) {
            return MiniGameResult(
                isCompleted = true,
                success = false,
                score = 0,
                finalProgress = 0.5f,
                message = "The merchant grows impatient and ends the negotiation."
            )
        }
        
        return null
    }
    
    override fun getProgress(state: MiniGameState): Float {
        val round = state.getData<Int>("round") ?: 1
        return (round - 1).toFloat() / MAX_ROUNDS
    }
    
    private fun calculateNPCResponse(
        approach: NegotiationApproach, 
        currentMood: Int, 
        round: Int
    ): NPCResponse {
        val baseResponse = when (npcPersonality) {
            NPCPersonality.GREEDY -> getGreedyResponse(approach)
            NPCPersonality.FRIENDLY -> getFriendlyResponse(approach)
            NPCPersonality.STUBBORN -> getStubbornResponse(approach)
            NPCPersonality.BALANCED -> getBalancedResponse(approach)
        }
        
        // Modify based on current mood and round
        val moodModifier = when {
            currentMood >= 2 -> 0.5f // More receptive when happy
            currentMood <= -1 -> -0.5f // Less receptive when annoyed
            else -> 0f
        }
        
        val roundModifier = if (round > 4) -0.3f else 0f // Gets impatient
        
        val finalPriceChange = (baseResponse.priceChange * (1f + moodModifier + roundModifier)).toInt()
        val finalMoodChange = baseResponse.moodChange + if (round > 4) -1 else 0
        
        return baseResponse.copy(
            priceChange = finalPriceChange,
            moodChange = finalMoodChange
        )
    }
    
    private fun getGreedyResponse(approach: NegotiationApproach): NPCResponse {
        return when (approach) {
            NegotiationApproach.POLITE_REQUEST -> NPCResponse(-5, 0, "I suppose I could come down a little...")
            NegotiationApproach.FIRM_COUNTER -> NPCResponse(-15, -1, "That's quite a bit lower than I was thinking...")
            NegotiationApproach.WALK_AWAY_THREAT -> NPCResponse(-20, -2, "Wait! Perhaps we can work something out...")
            NegotiationApproach.COMPLIMENT_FLATTERY -> NPCResponse(-8, 1, "Well, you certainly know quality when you see it!")
            NegotiationApproach.POINT_OUT_FLAWS -> NPCResponse(-12, -1, "Those are minor imperfections at best!")
        }
    }
    
    private fun getFriendlyResponse(approach: NegotiationApproach): NPCResponse {
        return when (approach) {
            NegotiationApproach.POLITE_REQUEST -> NPCResponse(-15, 1, "Since you asked so nicely, how about this price?")
            NegotiationApproach.FIRM_COUNTER -> NPCResponse(-10, 0, "That's reasonable, I can work with that.")
            NegotiationApproach.WALK_AWAY_THREAT -> NPCResponse(-5, -2, "No need to be hasty! We can find a middle ground.")
            NegotiationApproach.COMPLIMENT_FLATTERY -> NPCResponse(-12, 2, "You're too kind! Let me give you a better price.")
            NegotiationApproach.POINT_OUT_FLAWS -> NPCResponse(-8, -1, "You have a good eye... I'll adjust accordingly.")
        }
    }
    
    private fun getStubbornResponse(approach: NegotiationApproach): NPCResponse {
        return when (approach) {
            NegotiationApproach.POLITE_REQUEST -> NPCResponse(-3, 0, "The price is the price, but... fine, a small discount.")
            NegotiationApproach.FIRM_COUNTER -> NPCResponse(-8, -1, "That's much too low for an item of this quality!")
            NegotiationApproach.WALK_AWAY_THREAT -> NPCResponse(-15, -1, "I don't respond well to threats, but I need the sale...")
            NegotiationApproach.COMPLIMENT_FLATTERY -> NPCResponse(-5, 0, "Flattery won't change the value, but... I appreciate it.")
            NegotiationApproach.POINT_OUT_FLAWS -> NPCResponse(-10, -2, "Are you suggesting I don't know my own merchandise?!")
        }
    }
    
    private fun getBalancedResponse(approach: NegotiationApproach): NPCResponse {
        return when (approach) {
            NegotiationApproach.POLITE_REQUEST -> NPCResponse(-8, 0, "I can offer a modest discount.")
            NegotiationApproach.FIRM_COUNTER -> NPCResponse(-12, -1, "That's lower than I'd like, but not unreasonable.")
            NegotiationApproach.WALK_AWAY_THREAT -> NPCResponse(-10, -1, "Let's see if we can reach an agreement first.")
            NegotiationApproach.COMPLIMENT_FLATTERY -> NPCResponse(-7, 1, "I appreciate that! Perhaps a small adjustment...")
            NegotiationApproach.POINT_OUT_FLAWS -> NPCResponse(-9, 0, "You make a fair point. Let me reconsider the price.")
        }
    }
    
    private fun calculateMood(moodScore: Int): NPCMood {
        return when {
            moodScore >= 3 -> NPCMood.HAPPY
            moodScore >= 0 -> NPCMood.NEUTRAL
            moodScore >= -2 -> NPCMood.ANNOYED
            else -> NPCMood.ANGRY
        }
    }
    
    private fun formatNegotiationRound(approach: NegotiationApproach, response: NPCResponse, newPrice: Int): String {
        val approachText = when (approach) {
            NegotiationApproach.POLITE_REQUEST -> "You politely ask for a lower price"
            NegotiationApproach.FIRM_COUNTER -> "You make a firm counteroffer"
            NegotiationApproach.WALK_AWAY_THREAT -> "You threaten to walk away"
            NegotiationApproach.COMPLIMENT_FLATTERY -> "You compliment the merchant"
            NegotiationApproach.POINT_OUT_FLAWS -> "You point out flaws in the item"
        }
        return "$approachText - ${response.message} (New price: $newPrice coins)"
    }
    
    private fun calculateFinalScore(savings: Int, originalPrice: Int, attempts: Int): Int {
        val savingsPercent = (savings.toFloat() / originalPrice) * 100
        val savingsScore = (savingsPercent * 10).toInt()
        val efficiencyBonus = maxOf(0, (MAX_ROUNDS - attempts) * 20)
        return savingsScore + efficiencyBonus
    }
    
    private fun getRewards(success: Boolean, savingsPercent: Float): List<String> {
        return if (success) {
            val rewards = mutableListOf("coins", "experience")
            if (savingsPercent >= 30) {
                rewards.add("reputation") // Great negotiator reputation
            }
            rewards
        } else {
            emptyList()
        }
    }
    
    private data class NPCResponse(
        val priceChange: Int,
        val moodChange: Int,
        val message: String
    )
} 