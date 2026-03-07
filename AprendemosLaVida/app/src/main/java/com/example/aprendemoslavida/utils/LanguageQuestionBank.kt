package com.example.aprendemoslavida.utils

import com.example.aprendemoslavida.model.Question

object LanguageQuestionBank {
    fun allQuestions(): List<Question> {
        return listOf(
            Question("¿Qué parte de la oración realiza la acción?", listOf("Sujeto", "Predicado", "Verbo", "Artículo"), 0),
            Question("En 'La niña canta', ¿cuál es el verbo?", listOf("canta", "niña", "la", "la niña"), 0),
            Question("En 'El perro corre rápido', el predicado es...", listOf("corre rápido", "El perro", "perro", "rápido"), 0),
            Question("¿Qué opción es un sinónimo de 'feliz'?", listOf("contento", "triste", "enfadado", "lento"), 0),
            Question("¿Qué opción es un antónimo de 'grande'?", listOf("pequeño", "alto", "ancho", "fuerte"), 0),
            Question("¿Cuál es una palabra monosílaba?", listOf("sol", "casa", "mesa", "luna"), 0),
            Question("¿Cuál es una palabra bisílaba?", listOf("cama", "pan", "tren", "flor"), 0),
            Question("Singular de 'árboles' es...", listOf("árbol", "árbola", "árbols", "árbeles"), 0),
            Question("Plural de 'flor' es...", listOf("flores", "flors", "floras", "flore"), 0),
            Question("¿Cuál es un nombre colectivo?", listOf("manada", "perro", "niña", "árbol"), 0),
            Question("¿Cuál es un nombre individual?", listOf("oveja", "rebaño", "equipo", "familia"), 0),
            Question("¿Cuál es un nombre propio?", listOf("María", "ciudad", "niño", "colegio"), 0),
            Question("¿Cuál es un nombre común?", listOf("perro", "Pedro", "España", "Marta"), 0),
            Question("En 'Mis amigos juegan', el sujeto es...", listOf("Mis amigos", "juegan", "mis", "amigos juegan"), 0),
            Question("En 'Ana lee un cuento', el predicado es...", listOf("lee un cuento", "Ana", "cuento", "un cuento"), 0),
            Question("Sinónimo de 'rápido' es...", listOf("veloz", "torpe", "lento", "débil"), 0),
            Question("Antónimo de 'frío' es...", listOf("caliente", "helado", "fresco", "húmedo"), 0),
            Question("¿Cuál es monosílaba?", listOf("mar", "rana", "mesa", "silla"), 0),
            Question("¿Cuál es trisílaba?", listOf("pelota", "flor", "sol", "tren"), 0),
            Question("Plural de 'lápiz' es...", listOf("lápices", "lápizs", "lápizes", "lápizes"), 0),
            Question("Singular de 'papeles' es...", listOf("papel", "papela", "papele", "papels"), 0),
            Question("¿Qué nombre es colectivo de peces?", listOf("banco", "pez", "pecera", "marea"), 0),
            Question("¿Qué nombre es individual de 'bosque'?", listOf("árbol", "rama", "hoja", "tronco"), 0),
            Question("¿Qué nombre es propio?", listOf("Lugo", "provincia", "río", "montaña"), 0),
            Question("¿Qué nombre es común?", listOf("ciudad", "Madrid", "Lucas", "Galicia"), 0),
            Question("En 'Los pájaros vuelan', el verbo es...", listOf("vuelan", "los", "pájaros", "los pájaros"), 0),
            Question("En 'Mi madre cocina', el sujeto es...", listOf("Mi madre", "cocina", "madre", "mi"), 0),
            Question("Sinónimo de 'bonito' es...", listOf("hermoso", "feo", "sucio", "gris"), 0),
            Question("Antónimo de 'encendido' es...", listOf("apagado", "brillante", "luminoso", "claro"), 0),
            Question("¿Cuál es bisílaba?", listOf("mesa", "sol", "tren", "pan"), 0),
            Question("¿Cuál es monosílaba?", listOf("pie", "sapo", "coche", "lápiz"), 0),
            Question("Plural de 'animal' es...", listOf("animales", "animals", "animaleses", "animalez"), 0),
            Question("Singular de 'camiones' es...", listOf("camión", "camione", "camion", "camións"), 0),
            Question("¿Qué es 'equipo'?", listOf("nombre colectivo", "nombre individual", "verbo", "adjetivo"), 0),
            Question("¿Qué es 'jugador'?", listOf("nombre individual", "nombre colectivo", "verbo", "adverbio"), 0),
            Question("¿Qué oración tiene sujeto y predicado correctos?", listOf("El gato duerme", "Duerme el", "Gato el duerme", "El duerme gato"), 0),
            Question("En 'Pedro y Laura estudian', el sujeto es...", listOf("Pedro y Laura", "estudian", "Pedro", "Laura"), 0),
            Question("¿Cuál NO es antónimo de 'alto'?", listOf("grande", "bajo", "pequeño", "corto"), 0),
            Question("¿Cuál es un sinónimo de 'hablar'?", listOf("conversar", "callar", "dormir", "saltar"), 0),
            Question("En 'Nosotros jugamos fútbol', el predicado es...", listOf("jugamos fútbol", "Nosotros", "fútbol", "Nosotros jugamos"), 0)
        )
    }
}

