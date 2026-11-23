package com.example.aprendemoslavida.model

/**
 * Perfil del avatar (solo se guarda en el móvil).
 * - Por defecto: niña con la ropa básica (outfit = -1) y el calzado básico (shoes = 0).
 * - outfit: -1 = ropa básica común; 0..6 = prendas del género (la 6 es la de gala).
 * - shoes: 0 = calzado básico común; 1..5 = calzado del género.
 * - name: nombre del avatar (vacío = sin nombre); Sofía lo usa al saludar.
 */
data class AvatarProfile(
    var gender: String = "NINA",
    var hairStyle: Int = 0,
    var hairColor: Int = 0,
    var eyeShape: Int = 0,
    var eyeColor: Int = 0,
    var mouthShape: Int = 0,
    var mouthColor: Int = 0,
    var skinTone: Int = 0,
    var outfit: Int = -1,
    var shoes: Int = 0,
    var name: String = ""
)
