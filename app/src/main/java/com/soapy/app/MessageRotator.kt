package com.soapy.app

enum class MessageCategory { TOUGH_LOVE, ENCOURAGEMENT, REMINDER, FAITH }

data class BlockMessage(val text: String, val category: MessageCategory)

/**
 * Bundled starter messages. Later versions will refresh this set from the admin
 * panel, and keep this list as the offline fallback.
 */
object MessageRotator {

    private const val AVOID_LAST = 5

    private val messages = listOf(
        BlockMessage("Boy, you should be doing better things with your life, not this.", MessageCategory.TOUGH_LOVE),
        BlockMessage("You didn't come this far to go back. Put the phone down and go build something.", MessageCategory.TOUGH_LOVE),
        BlockMessage("This urge will pass in minutes. The regret will last much longer.", MessageCategory.TOUGH_LOVE),
        BlockMessage("Oga, abeg no waste your future for this thing. You dey better pass am.", MessageCategory.TOUGH_LOVE),

        BlockMessage("Every time you stop, you get stronger. Keep going.", MessageCategory.ENCOURAGEMENT),
        BlockMessage("You are not weak for feeling the urge. You are strong for stopping.", MessageCategory.ENCOURAGEMENT),
        BlockMessage("Small wins add up. This is one of them.", MessageCategory.ENCOURAGEMENT),
        BlockMessage("You don win this one. Make we keep going.", MessageCategory.ENCOURAGEMENT),

        BlockMessage("Take a deep breath. In for 4, hold for 4, out for 4. The urge fades.", MessageCategory.REMINDER),
        BlockMessage("Remember why you started.", MessageCategory.REMINDER),
        BlockMessage("Drink water, stand up, walk around. Change what you are doing for five minutes.", MessageCategory.REMINDER),
        BlockMessage("Go outside small, stretch your body. The urge go pass.", MessageCategory.REMINDER),

        BlockMessage("You are stronger than this moment. Pray if you can, then take the next right step.", MessageCategory.FAITH)
    )

    private val recent = ArrayDeque<Int>()

    @Synchronized
    fun next(): BlockMessage {
        val candidates = messages.indices.filter { it !in recent }
        val index = candidates.random()
        recent.addLast(index)
        while (recent.size > AVOID_LAST) recent.removeFirst()
        return messages[index]
    }
}
