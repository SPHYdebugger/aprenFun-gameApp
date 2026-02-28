package com.example.aprendemoslavida.utils

import android.content.Context
import com.example.aprendemoslavida.R
import com.example.aprendemoslavida.model.Question

class EnglishGameManager(context: Context) {
    private val questions: List<Question> = buildQuestions(context)
        .shuffled()
        .take(20)

    var currentIndex: Int = 0
        private set
    var score: Int = 0
        private set
    var totalTimeMs: Int = 0
        private set
    var correctAnswers: Int = 0
        private set

    fun totalQuestions(): Int = questions.size

    fun currentQuestion(): Question? = questions.getOrNull(currentIndex)

    fun addScore(points: Int) {
        score += points
    }

    fun addTime(ms: Int) {
        totalTimeMs += ms
    }

    fun addCorrect() {
        correctAnswers += 1
    }

    fun moveNext(): Boolean {
        currentIndex += 1
        return currentIndex < questions.size
    }

    fun pointsForElapsed(elapsedMs: Int): Int {
        return when {
            elapsedMs < 4000 -> 100
            elapsedMs < 8000 -> 70
            elapsedMs < 12000 -> 40
            else -> 0
        }
    }

    private fun buildQuestions(context: Context): List<Question> {
        return listOf(
            Question("What place can you watch a movie? 🎬", listOf("Cinema", "Hospital", "Post office", "Museum"), 0),
            Question("Where do firefighters work? 🚒", listOf("Fire station", "Cinema", "School", "Museum"), 0),
            Question("Where do you send letters? ✉️", listOf("Post office", "Hospital", "Cinema", "Swimming pool"), 0),
            Question("Where can you swim? 🏊", listOf("Swimming pool", "Museum", "Fire station", "Train station"), 0),
            Question("Where do trains stop? 🚆", listOf("Train station", "Cinema", "Hospital", "Post office"), 0),
            Question("Where can you see old things? 🏛️", listOf("Museum", "Swimming pool", "Fire station", "Shop"), 0),
            Question("Where can you buy many things? 🛍️", listOf("Shopping centre", "Hospital", "Museum", "Fire station"), 0),
            Question("Which place helps sick people? 🏥", listOf("Hospital", "Cinema", "Museum", "Post office"), 0),
            Question("Is there a cinema in the town? 🎥", listOf("Yes, there is", "No, there isn’t", "Yes, it is", "No, it is"), 0),
            Question("Is there a hospital in the town? 🏥", listOf("Yes, there is", "No, there isn’t", "Yes, it does", "No, it does"), 0),
            Question("How do we ask about places?", listOf("Is there a…?", "There are a…", "Do you there…?", "Is it a…?"), 0),
            Question("Negative form of 'There is'?", listOf("There isn’t", "There aren’t", "There not", "Isn’t there"), 0),
            Question("Positive form?", listOf("There is", "Is there", "There are not", "There be"), 0),
            Question("A city is usually… 🏙️", listOf("Big", "Small", "Empty", "Quiet"), 0),
            Question("A village is usually… 🏡", listOf("Small", "Very big", "Noisy", "Crowded"), 0),
            Question("A country has… 🌄", listOf("Cities and villages", "Only houses", "Only schools", "Only shops"), 0),
            Question("Where do you live? 🏠", listOf("City / town / village", "Cinema", "Hospital", "Museum"), 0),
            Question("Where do you go swimming?", listOf("Swimming pool", "Post office", "Fire station", "Museum"), 0),
            Question("Where can you see firefighters? 🚒", listOf("Fire station", "Hospital", "Cinema", "Shop"), 0),
            Question("Recycle means… ♻️", listOf("Use again", "Throw away", "Break", "Lose"), 0),
            Question("Paper goes in the… 📰", listOf("Paper bin", "Plastic bin", "Glass bin", "Food bin"), 0),
            Question("Plastic goes in the… 🧴", listOf("Plastic bin", "Paper bin", "Glass bin", "Metal bin"), 0),
            Question("Glass goes in the… 🍾", listOf("Glass bin", "Plastic bin", "Paper bin", "Food bin"), 0),
            Question("Should we recycle? ♻️", listOf("Yes", "No", "Sometimes not", "Never"), 0),
            Question("What is a battery? 🔋", listOf("A small power object", "A toy", "Food", "A drink"), 0),
            Question("Where do batteries go?", listOf("Recycling box", "Paper bin", "Plastic bin", "Trash"), 0),
            Question("Is there a swimming pool? 🏊", listOf("Yes, there is", "No, there are", "Yes, it does", "No, it do"), 0),
            Question("Is there a train station? 🚆", listOf("Yes, there is", "No, there aren’t", "Yes, they are", "No, it are"), 0),
            Question("There ___ a museum.", listOf("is", "are", "isn’t", "aren’t"), 0),
            Question("There ___ a hospital.", listOf("is", "are", "aren’t", "were"), 0),
            Question("There ___ a cinema.", listOf("is", "are", "was", "were"), 0),
            Question("Which is NOT a place in town?", listOf("Forest", "Cinema", "Hospital", "Museum"), 0),
            Question("Which is a place in town?", listOf("Post office", "Mountain", "River", "Beach"), 0),
            Question("We go to the cinema to… 🎬", listOf("Watch movies", "Eat lunch", "Sleep", "Study"), 0),
            Question("We go to the hospital to… 🏥", listOf("See a doctor", "Buy food", "Watch films", "Play games"), 0),
            Question("Is there a fire station? 🚒", listOf("Yes, there is", "No, it is", "Yes, they are", "No, there are"), 0),
            Question("There isn’t a cinema. That means…", listOf("No cinema", "Two cinemas", "Big cinema", "Old cinema"), 0),
            Question("Which place has trains? 🚆", listOf("Train station", "Cinema", "Hospital", "Museum"), 0),
            Question("Which place sells things?", listOf("Shopping centre", "Hospital", "Fire station", "Museum"), 0),
            Question("City, town, village are…", listOf("Places to live", "Jobs", "Buildings", "Objects"), 0),
            Question("Which sentence is correct?", listOf("There is a cinema", "There are a cinema", "Is there cinema", "There cinema"), 0),
            Question("Which sentence is correct?", listOf("Is there a hospital?", "Is hospital there?", "There is hospital?", "Is a hospital there is?"), 0),

            Question(
                context.getString(R.string.eng_q_post_offices_city),
                listOf(
                    "There are two post offices in my city",
                    "There is two post offices in my city",
                    "There are two post office in my city",
                    "There is two post office in my city"
                ),
                0
            ),
            Question(
                context.getString(R.string.eng_q_swimming_pools_town),
                listOf(
                    "There are two swimming pools in my town",
                    "There is two swimming pools in my town",
                    "There are two swimming pool in my town",
                    "There is two swimming pool in my town"
                ),
                0
            ),
            Question(
                context.getString(R.string.eng_q_place_to_buy),
                listOf(
                    "Shopping centre",
                    "Fire station",
                    "Post office",
                    "Museum"
                ),
                0
            ),
            Question(
                context.getString(R.string.eng_q_is_there_cinema_town),
                listOf(
                    "Is there a cinema in your town?",
                    "There is a cinema in your town?",
                    "Is cinema there in your town?",
                    "Are there a cinema in your town?"
                ),
                0
            ),
            Question(
                context.getString(R.string.eng_q_there_is_hospital_town),
                listOf(
                    "There is a hospital in my town",
                    "There are a hospital in my town",
                    "There is hospital my town",
                    "Is there a hospital in my town"
                ),
                0
            ),
            Question(
                context.getString(R.string.eng_q_no_museum_city),
                listOf(
                    "There isn’t a museum in my city",
                    "There aren’t a museum in my city",
                    "There isn’t museum my city",
                    "Isn’t there a museum in my city"
                ),
                0
            ),
            Question(
                context.getString(R.string.eng_q_train_station_question),
                listOf(
                    "Is there a train station?",
                    "There is a train station?",
                    "Is train station there?",
                    "Are there a train station?"
                ),
                0
            ),
            Question(
                context.getString(R.string.eng_q_pool_town),
                listOf(
                    "There is a swimming pool in my town",
                    "There are a swimming pool in my town",
                    "There is swimming pool my town",
                    "Is there swimming pool my town"
                ),
                0
            ),
            Question(
                context.getString(R.string.eng_q_no_cinema_city),
                listOf(
                    "There isn’t a cinema in my city",
                    "There aren’t a cinema in my city",
                    "There isn’t cinema my city",
                    "Isn’t there cinema in my city"
                ),
                0
            ),
            Question(
                context.getString(R.string.eng_q_two_hospitals_city),
                listOf(
                    "There are two hospitals in my city",
                    "There is two hospitals in my city",
                    "There are two hospital in my city",
                    "There is two hospital in my city"
                ),
                0
            ),
            Question(
                context.getString(R.string.eng_q_post_office_question),
                listOf(
                    "Is there a post office?",
                    "There is a post office?",
                    "Is post office there?",
                    "Are there a post office?"
                ),
                0
            ),
            Question(
                context.getString(R.string.eng_q_cinema_museum_town),
                listOf(
                    "There is a cinema and a museum in my town",
                    "There are a cinema and a museum in my town",
                    "There is cinema and museum my town",
                    "There are cinema and museum my town"
                ),
                0
            ),
            Question(
                context.getString(R.string.eng_q_many_shops_city),
                listOf(
                    "There are many shops in my city",
                    "There is many shops in my city",
                    "There are much shops in my city",
                    "There is much shops in my city"
                ),
                0
            ),
            Question(
                context.getString(R.string.eng_q_shopping_centre_question),
                listOf(
                    "Is there a shopping centre?",
                    "There is a shopping centre?",
                    "Is shopping centre there?",
                    "Are there shopping centre?"
                ),
                0
            ),
            Question(
                context.getString(R.string.eng_q_no_train_station_town),
                listOf(
                    "There isn’t a train station in my town",
                    "There aren’t a train station in my town",
                    "There isn’t train station my town",
                    "Isn’t there train station my town"
                ),
                0
            ),
            Question(
                context.getString(R.string.eng_q_park_city),
                listOf(
                    "There is a park in my city",
                    "There are a park in my city",
                    "There is park my city",
                    "Is there park my city"
                ),
                0
            ),
            Question(
                context.getString(R.string.eng_q_hospitals_city_question),
                listOf(
                    "Are there hospitals in your city?",
                    "Is there hospitals in your city?",
                    "Are there hospital in your city?",
                    "Is there hospital in your city?"
                ),
                0
            ),
            Question(
                context.getString(R.string.eng_q_two_cinemas_town),
                listOf(
                    "There are two cinemas in my town",
                    "There is two cinemas in my town",
                    "There are two cinema in my town",
                    "There is two cinema in my town"
                ),
                0
            ),
            Question(
                context.getString(R.string.eng_q_fire_station_question),
                listOf(
                    "Is there a fire station?",
                    "There is a fire station?",
                    "Is fire station there?",
                    "Are there fire station?"
                ),
                0
            ),
            Question(
                context.getString(R.string.eng_q_museum_city),
                listOf(
                    "There is a museum in my city",
                    "There are a museum in my city",
                    "There is museum my city",
                    "Is there museum my city"
                ),
                0
            )
        )
    }
}
