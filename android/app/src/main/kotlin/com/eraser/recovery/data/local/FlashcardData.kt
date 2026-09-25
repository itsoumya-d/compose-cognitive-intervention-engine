package com.eraser.recovery.data.local

import com.eraser.recovery.data.local.entity.*
import java.time.LocalDateTime

/**
 * Flashcard Data
 * 
 * Contains all 60 default flashcards for the intervention system.
 * Migrated 50 flashcards from Flutter app + 10 new flashcards.
 * 
 * Distribution:
 * - Goal-Oriented: 12 flashcards (20%)
 * - Consequence Awareness: 10 flashcards (17%)
 * - Empowerment: 14 flashcards (23%)
 * - Mindfulness: 10 flashcards (17%)
 * - Redirection: 14 flashcards (23%)
 * 
 * Research findings from Qustodio, Net Nanny, Bark, Norton Family:
 * - Diverse task types for different user preferences
 * - Varied difficulty levels for progressive challenge
 * - Evidence-based intervention techniques
 * - Focus on behavioral activation and cognitive reframing
 */
object FlashcardData {
    
    fun getAllFlashcards(): List<FlashcardEntity> = listOf(
        // ========== GOAL-ORIENTED (12 flashcards - 20%) ==========
        FlashcardEntity(
            flashcardId = "goal_01",
            frontMessage = "What would your future self thank you for doing right now?",
            backTask = "Take 5 minutes to write down 3 goals you have for this week. Focus on who you want to become.",
            taskType = TaskType.REFLECTION,
            taskDuration = 5,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.GOAL_ORIENTED
        ),
        FlashcardEntity(
            flashcardId = "goal_02",
            frontMessage = "Remember why you started this journey",
            backTask = "Write a letter to yourself explaining why you want to break free from this habit. Keep it somewhere you can read it later.",
            taskType = TaskType.REFLECTION,
            taskDuration = 10,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.GOAL_ORIENTED
        ),
        FlashcardEntity(
            flashcardId = "goal_03",
            frontMessage = "Is this aligned with the person you want to become?",
            backTask = "Do 20 push-ups or squats. Physical strength builds mental strength.",
            taskType = TaskType.PHYSICAL,
            taskDuration = 5,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.GOAL_ORIENTED
        ),
        FlashcardEntity(
            flashcardId = "goal_04",
            frontMessage = "Think about your goals for the next 30 days",
            backTask = "Create a vision board or list of 5 things you want to accomplish this month. Make it visual and inspiring.",
            taskType = TaskType.CREATIVE,
            taskDuration = 15,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.GOAL_ORIENTED
        ),
        FlashcardEntity(
            flashcardId = "goal_05",
            frontMessage = "Your dreams are bigger than this moment",
            backTask = "Go for a 10-minute walk outside. Clear your head and think about your bigger purpose.",
            taskType = TaskType.PHYSICAL,
            taskDuration = 10,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.GOAL_ORIENTED
        ),
        FlashcardEntity(
            flashcardId = "goal_06",
            frontMessage = "Who do you want to be one year from now?",
            backTask = "Write down 5 qualities of your ideal self. Then choose one action you can take today to embody that quality.",
            taskType = TaskType.REFLECTION,
            taskDuration = 7,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.GOAL_ORIENTED
        ),
        FlashcardEntity(
            flashcardId = "goal_07",
            frontMessage = "Your goals deserve your full attention",
            backTask = "Spend 15 minutes working on a personal project or learning something new that aligns with your goals.",
            taskType = TaskType.CREATIVE,
            taskDuration = 15,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.GOAL_ORIENTED
        ),
        FlashcardEntity(
            flashcardId = "goal_08",
            frontMessage = "What legacy do you want to leave?",
            backTask = "Write a paragraph about the impact you want to have on the world. Read it aloud to yourself.",
            taskType = TaskType.REFLECTION,
            taskDuration = 8,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.GOAL_ORIENTED
        ),
        FlashcardEntity(
            flashcardId = "goal_09",
            frontMessage = "Success is built on small daily choices",
            backTask = "Do 30 jumping jacks or a 5-minute plank. Prove to yourself that you can do hard things.",
            taskType = TaskType.PHYSICAL,
            taskDuration = 5,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.GOAL_ORIENTED
        ),
        FlashcardEntity(
            flashcardId = "goal_10",
            frontMessage = "Your future is created by what you do today",
            backTask = "List 3 people you admire and why. Then identify one trait they have that you want to develop.",
            taskType = TaskType.REFLECTION,
            taskDuration = 6,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.GOAL_ORIENTED
        ),
        
        // NEW FLASHCARDS - Goal-Oriented
        FlashcardEntity(
            flashcardId = "goal_11",
            frontMessage = "What does success look like for you in 6 months?",
            backTask = "Create a detailed vision of your life 6 months from now. Write about your daily routine, relationships, and achievements.",
            taskType = TaskType.REFLECTION,
            taskDuration = 12,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.GOAL_ORIENTED
        ),
        FlashcardEntity(
            flashcardId = "goal_12",
            frontMessage = "Your potential is waiting to be unlocked",
            backTask = "Identify one skill you want to develop. Spend 20 minutes learning about it or practicing it right now.",
            taskType = TaskType.CREATIVE,
            taskDuration = 20,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.GOAL_ORIENTED
        ),
        
        // ========== CONSEQUENCE AWARENESS (10 flashcards - 17%) ==========
        FlashcardEntity(
            flashcardId = "consequence_01",
            frontMessage = "How will you feel in 10 minutes if you proceed?",
            backTask = "Take 10 deep breaths. Inhale for 4 counts, hold for 4, exhale for 6. Focus on the present moment.",
            taskType = TaskType.MINDFULNESS,
            taskDuration = 3,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.CONSEQUENCE_AWARENESS
        ),
        FlashcardEntity(
            flashcardId = "consequence_02",
            frontMessage = "What will this cost you emotionally?",
            backTask = "Journal for 5 minutes about how you felt after your last relapse. Be honest with yourself.",
            taskType = TaskType.REFLECTION,
            taskDuration = 5,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.CONSEQUENCE_AWARENESS
        ),
        FlashcardEntity(
            flashcardId = "consequence_03",
            frontMessage = "Remember the last time - was it worth it?",
            backTask = "Write down 3 negative consequences you experienced from this habit. Keep this list visible.",
            taskType = TaskType.REFLECTION,
            taskDuration = 5,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.CONSEQUENCE_AWARENESS
        ),
        FlashcardEntity(
            flashcardId = "consequence_04",
            frontMessage = "Think about who you might hurt",
            backTask = "Call or text someone you care about. Remind yourself of the relationships that matter.",
            taskType = TaskType.SOCIAL,
            taskDuration = 10,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.CONSEQUENCE_AWARENESS
        ),
        FlashcardEntity(
            flashcardId = "consequence_05",
            frontMessage = "This moment of pleasure leads to hours of regret",
            backTask = "Do 25 burpees or run in place for 3 minutes. Channel that energy into something positive.",
            taskType = TaskType.PHYSICAL,
            taskDuration = 5,
            difficultyLevel = DifficultyLevel.HARD,
            messageCategory = MessageCategory.CONSEQUENCE_AWARENESS
        ),
        FlashcardEntity(
            flashcardId = "consequence_06",
            frontMessage = "Your streak is worth more than this urge",
            backTask = "Review your progress in the app. Celebrate how far you've come. Don't throw it away.",
            taskType = TaskType.REFLECTION,
            taskDuration = 3,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.CONSEQUENCE_AWARENESS
        ),
        FlashcardEntity(
            flashcardId = "consequence_07",
            frontMessage = "What would you tell a friend in this situation?",
            backTask = "Write advice to yourself as if you were counseling a close friend. Be compassionate but honest.",
            taskType = TaskType.REFLECTION,
            taskDuration = 7,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.CONSEQUENCE_AWARENESS
        ),
        FlashcardEntity(
            flashcardId = "consequence_08",
            frontMessage = "The shame isn't worth the temporary escape",
            backTask = "Practice self-compassion. Write down 3 things you appreciate about yourself right now.",
            taskType = TaskType.REFLECTION,
            taskDuration = 5,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.CONSEQUENCE_AWARENESS
        ),
        
        // NEW FLASHCARDS - Consequence Awareness
        FlashcardEntity(
            flashcardId = "consequence_09",
            frontMessage = "How will this affect your sleep and energy tomorrow?",
            backTask = "Write about how this habit has impacted your physical health. List 3 ways you want to feel healthier.",
            taskType = TaskType.REFLECTION,
            taskDuration = 6,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.CONSEQUENCE_AWARENESS
        ),
        FlashcardEntity(
            flashcardId = "consequence_10",
            frontMessage = "What opportunities might you miss if you give in?",
            backTask = "List 5 positive things you could do with the time and energy you'd waste. Choose one and commit to it.",
            taskType = TaskType.REFLECTION,
            taskDuration = 8,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.CONSEQUENCE_AWARENESS
        ),

        // ========== EMPOWERMENT (14 flashcards - 23%) ==========
        FlashcardEntity(
            flashcardId = "empower_01",
            frontMessage = "You are stronger than this urge",
            backTask = "Do 30 push-ups or hold a plank for 2 minutes. Prove your strength to yourself.",
            taskType = TaskType.PHYSICAL,
            taskDuration = 5,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.EMPOWERMENT
        ),
        FlashcardEntity(
            flashcardId = "empower_02",
            frontMessage = "You've overcome this before, you can do it again",
            backTask = "Recall a time you successfully resisted this urge. Write down what helped you succeed.",
            taskType = TaskType.REFLECTION,
            taskDuration = 5,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.EMPOWERMENT
        ),
        FlashcardEntity(
            flashcardId = "empower_03",
            frontMessage = "Every \"no\" makes you stronger",
            backTask = "Go for a 15-minute walk or jog. Feel your physical and mental strength growing.",
            taskType = TaskType.PHYSICAL,
            taskDuration = 15,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.EMPOWERMENT
        ),
        FlashcardEntity(
            flashcardId = "empower_04",
            frontMessage = "You have the power to choose differently",
            backTask = "List 5 healthy activities you enjoy. Choose one and do it for 10 minutes right now.",
            taskType = TaskType.CREATIVE,
            taskDuration = 10,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.EMPOWERMENT
        ),
        FlashcardEntity(
            flashcardId = "empower_05",
            frontMessage = "You are not your urges - you are your choices",
            backTask = "Practice a 5-minute body scan meditation. Notice sensations without judgment.",
            taskType = TaskType.MINDFULNESS,
            taskDuration = 5,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.EMPOWERMENT
        ),
        FlashcardEntity(
            flashcardId = "empower_06",
            frontMessage = "Your willpower is a muscle - flex it now",
            backTask = "Do 50 jumping jacks or 3 minutes of high knees. Feel your power.",
            taskType = TaskType.PHYSICAL,
            taskDuration = 5,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.EMPOWERMENT
        ),
        FlashcardEntity(
            flashcardId = "empower_07",
            frontMessage = "You've already won by pausing",
            backTask = "Write down 3 victories you've had this week, no matter how small. Celebrate them.",
            taskType = TaskType.REFLECTION,
            taskDuration = 5,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.EMPOWERMENT
        ),
        FlashcardEntity(
            flashcardId = "empower_08",
            frontMessage = "Champions are made in moments like this",
            backTask = "Create a power playlist of 5 songs that make you feel strong. Listen to one right now.",
            taskType = TaskType.CREATIVE,
            taskDuration = 5,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.EMPOWERMENT
        ),
        FlashcardEntity(
            flashcardId = "empower_09",
            frontMessage = "You are capable of incredible things",
            backTask = "Write a list of 10 things you've accomplished in your life. Read it and feel proud.",
            taskType = TaskType.REFLECTION,
            taskDuration = 7,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.EMPOWERMENT
        ),
        FlashcardEntity(
            flashcardId = "empower_10",
            frontMessage = "This is your moment to be the hero of your story",
            backTask = "Do a cold shower for 2 minutes or splash cold water on your face for 30 seconds. Reset your system.",
            taskType = TaskType.PHYSICAL,
            taskDuration = 3,
            difficultyLevel = DifficultyLevel.HARD,
            messageCategory = MessageCategory.EMPOWERMENT
        ),
        FlashcardEntity(
            flashcardId = "empower_11",
            frontMessage = "You control your destiny",
            backTask = "Write your personal mission statement in one sentence. Make it powerful and inspiring.",
            taskType = TaskType.REFLECTION,
            taskDuration = 8,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.EMPOWERMENT
        ),
        FlashcardEntity(
            flashcardId = "empower_12",
            frontMessage = "Strength isn't never falling - it's getting back up",
            backTask = "Do 40 squats or lunges. Feel your legs burn and know you can endure discomfort.",
            taskType = TaskType.PHYSICAL,
            taskDuration = 5,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.EMPOWERMENT
        ),

        // NEW FLASHCARDS - Empowerment
        FlashcardEntity(
            flashcardId = "empower_13",
            frontMessage = "You've already proven you can resist - do it again",
            backTask = "Review your streak history. Count every time you've said no. That's your power - use it now.",
            taskType = TaskType.REFLECTION,
            taskDuration = 4,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.EMPOWERMENT
        ),
        FlashcardEntity(
            flashcardId = "empower_14",
            frontMessage = "Your self-respect is earned in moments like this",
            backTask = "Stand in a power pose for 2 minutes. Feel your confidence grow. You are in control.",
            taskType = TaskType.PHYSICAL,
            taskDuration = 2,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.EMPOWERMENT
        ),

        // ========== MINDFULNESS (10 flashcards - 17%) ==========
        FlashcardEntity(
            flashcardId = "mindful_01",
            frontMessage = "What are you really feeling right now?",
            backTask = "Sit quietly for 5 minutes. Name 3 emotions you're experiencing without judging them.",
            taskType = TaskType.MINDFULNESS,
            taskDuration = 5,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.MINDFULNESS
        ),
        FlashcardEntity(
            flashcardId = "mindful_02",
            frontMessage = "Take a moment to breathe and observe this urge",
            backTask = "Practice box breathing: Inhale 4, hold 4, exhale 4, hold 4. Repeat for 5 minutes.",
            taskType = TaskType.MINDFULNESS,
            taskDuration = 5,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.MINDFULNESS
        ),
        FlashcardEntity(
            flashcardId = "mindful_03",
            frontMessage = "Notice the urge without acting on it",
            backTask = "Practice urge surfing: Observe the urge like a wave. Notice it rise, peak, and fall without acting.",
            taskType = TaskType.MINDFULNESS,
            taskDuration = 10,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.MINDFULNESS
        ),
        FlashcardEntity(
            flashcardId = "mindful_04",
            frontMessage = "This feeling will pass",
            backTask = "Set a timer for 10 minutes. Sit with the discomfort. Notice how it changes over time.",
            taskType = TaskType.MINDFULNESS,
            taskDuration = 10,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.MINDFULNESS
        ),
        FlashcardEntity(
            flashcardId = "mindful_05",
            frontMessage = "Where do you feel this urge in your body?",
            backTask = "Do a body scan. Notice tension, warmth, or sensations. Breathe into those areas.",
            taskType = TaskType.MINDFULNESS,
            taskDuration = 7,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.MINDFULNESS
        ),
        FlashcardEntity(
            flashcardId = "mindful_06",
            frontMessage = "You are not your thoughts",
            backTask = "Watch your thoughts like clouds passing. Don't engage, just observe for 5 minutes.",
            taskType = TaskType.MINDFULNESS,
            taskDuration = 5,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.MINDFULNESS
        ),
        FlashcardEntity(
            flashcardId = "mindful_07",
            frontMessage = "What triggered this urge?",
            backTask = "Journal about what happened in the last hour. Identify your trigger and how to avoid it next time.",
            taskType = TaskType.REFLECTION,
            taskDuration = 8,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.MINDFULNESS
        ),
        FlashcardEntity(
            flashcardId = "mindful_08",
            frontMessage = "Be present in this moment",
            backTask = "Use your 5 senses: Name 5 things you see, 4 you hear, 3 you feel, 2 you smell, 1 you taste.",
            taskType = TaskType.MINDFULNESS,
            taskDuration = 3,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.MINDFULNESS
        ),

        // NEW FLASHCARDS - Mindfulness
        FlashcardEntity(
            flashcardId = "mindful_09",
            frontMessage = "What need is this urge trying to meet?",
            backTask = "Reflect on what you're really seeking: connection, excitement, escape? Find a healthier way to meet that need.",
            taskType = TaskType.REFLECTION,
            taskDuration = 7,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.MINDFULNESS
        ),
        FlashcardEntity(
            flashcardId = "mindful_10",
            frontMessage = "Observe this moment without judgment",
            backTask = "Practice loving-kindness meditation for 5 minutes. Send compassion to yourself and others.",
            taskType = TaskType.MINDFULNESS,
            taskDuration = 5,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.MINDFULNESS
        ),

        // ========== REDIRECTION (14 flashcards - 23%) ==========
        FlashcardEntity(
            flashcardId = "redirect_01",
            frontMessage = "What healthy activity could you do instead?",
            backTask = "Choose one: Read for 15 minutes, practice an instrument, draw, or work on a hobby.",
            taskType = TaskType.CREATIVE,
            taskDuration = 15,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.REDIRECTION
        ),
        FlashcardEntity(
            flashcardId = "redirect_02",
            frontMessage = "Who could you reach out to right now?",
            backTask = "Call or text your accountability partner or a supportive friend. Have a real conversation.",
            taskType = TaskType.SOCIAL,
            taskDuration = 10,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.REDIRECTION
        ),
        FlashcardEntity(
            flashcardId = "redirect_03",
            frontMessage = "What would make you proud of yourself today?",
            backTask = "Complete one task you've been putting off. Clean your room, do dishes, or organize something.",
            taskType = TaskType.CREATIVE,
            taskDuration = 20,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.REDIRECTION
        ),
        FlashcardEntity(
            flashcardId = "redirect_04",
            frontMessage = "Choose a better path forward",
            backTask = "Go outside and take a 20-minute walk in nature. Leave your phone behind if possible.",
            taskType = TaskType.PHYSICAL,
            taskDuration = 20,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.REDIRECTION
        ),
        FlashcardEntity(
            flashcardId = "redirect_05",
            frontMessage = "Your energy could be used for something amazing",
            backTask = "Work on a personal project for 30 minutes. Make progress on something that matters to you.",
            taskType = TaskType.CREATIVE,
            taskDuration = 30,
            difficultyLevel = DifficultyLevel.HARD,
            messageCategory = MessageCategory.REDIRECTION
        ),
        FlashcardEntity(
            flashcardId = "redirect_06",
            frontMessage = "What skill have you wanted to learn?",
            backTask = "Watch an educational video or read an article about something you want to learn. Take notes.",
            taskType = TaskType.CREATIVE,
            taskDuration = 15,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.REDIRECTION
        ),
        FlashcardEntity(
            flashcardId = "redirect_07",
            frontMessage = "How can you serve someone else right now?",
            backTask = "Do something kind for someone: Send an encouraging message, help with a task, or make a donation.",
            taskType = TaskType.SOCIAL,
            taskDuration = 10,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.REDIRECTION
        ),
        FlashcardEntity(
            flashcardId = "redirect_08",
            frontMessage = "Your body needs movement, not pixels",
            backTask = "Do a full workout: 20 push-ups, 30 squats, 40 jumping jacks, 1-minute plank. Repeat 3 times.",
            taskType = TaskType.PHYSICAL,
            taskDuration = 15,
            difficultyLevel = DifficultyLevel.HARD,
            messageCategory = MessageCategory.REDIRECTION
        ),
        FlashcardEntity(
            flashcardId = "redirect_09",
            frontMessage = "What creative outlet calls to you?",
            backTask = "Spend 20 minutes being creative: Write, draw, play music, cook, or build something.",
            taskType = TaskType.CREATIVE,
            taskDuration = 20,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.REDIRECTION
        ),
        FlashcardEntity(
            flashcardId = "redirect_10",
            frontMessage = "Your mind needs nourishment, not numbing",
            backTask = "Read 10 pages of an inspiring book or listen to a motivational podcast episode.",
            taskType = TaskType.CREATIVE,
            taskDuration = 15,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.REDIRECTION
        ),
        FlashcardEntity(
            flashcardId = "redirect_11",
            frontMessage = "Connect with your community",
            backTask = "Join an online support group or forum. Share your struggle and encourage someone else.",
            taskType = TaskType.SOCIAL,
            taskDuration = 15,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.REDIRECTION
        ),
        FlashcardEntity(
            flashcardId = "redirect_12",
            frontMessage = "What would your best self do right now?",
            backTask = "Write down 5 characteristics of your best self. Choose one action that embodies that quality and do it.",
            taskType = TaskType.REFLECTION,
            taskDuration = 10,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.REDIRECTION
        ),

        // NEW FLASHCARDS - Redirection
        FlashcardEntity(
            flashcardId = "redirect_13",
            frontMessage = "Transform this energy into productivity",
            backTask = "Tackle your most challenging task for the day. Work on it for 25 minutes with full focus (Pomodoro technique).",
            taskType = TaskType.CREATIVE,
            taskDuration = 25,
            difficultyLevel = DifficultyLevel.HARD,
            messageCategory = MessageCategory.REDIRECTION
        ),
        FlashcardEntity(
            flashcardId = "redirect_14",
            frontMessage = "Your future self needs you to act now",
            backTask = "Plan your next 24 hours in detail. Schedule activities that align with your recovery goals.",
            taskType = TaskType.REFLECTION,
            taskDuration = 10,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.REDIRECTION
        ),
    )
}

