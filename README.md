# TripLog — Km Registratie voor Android Auto

Android-app die ritten registreert (zakelijk / privé) via Android Auto. Bij het aansluiten op de auto verschijnt een melding waarmee je het type rit kiest. De km-stand wordt automatisch opgehaald via de Volvo Connected Vehicle API.

---

## Wat heb je nodig

| Gegeven | Waar te vinden |
|---|---|
| **VIN** | Kentekenkaart of Volvo-app |
| **VCC-Api-Key** | [developer.volvocars.com](https://developer.volvocars.com) → jouw applicatie → Primary key |
| **Client ID** | Zelfde pagina op het developer portal |
| **Client Secret** | Zelfde pagina op het developer portal |

---

## App installeren op een nieuwe telefoon

### 1. Bouwen vanuit Android Studio

1. Clone de repo: `git clone git@github.com:ryklebaron/androidKmRegistratie.git`
2. Open het project in Android Studio
3. Sluit de telefoon aan via USB
4. Klik **Run** (of `./gradlew installDebug` in de terminal)

### 2. Android-permissies instellen

Open de app → **Instellingen**. Los eventuele rode banners op:

- **Meldingen uitgeschakeld** → tik de knop, schakel in
- **Schermactivering uitgeschakeld** → tik de knop, geef toestemming
- **Batterijoptimalisatie actief** → tik de knop, stel in op *Onbeperkt*

Zonder deze drie permissies start de rit-popup niet op bij verbinding met Android Auto.

### 3. Volvo-gegevens invullen

1. Ga naar **Instellingen** in de app
2. Vul in: VIN, VCC-Api-Key, Client ID, Client Secret
3. Tik **Opslaan**

### 4. Volvo-account koppelen (OAuth2)

1. Tik **Koppelen met Volvo** — de browser opent de Volvo-loginpagina
2. Log in met je Volvo-account
3. De browser stuurt je door naar een pagina op GitHub (404 is normaal)
4. Kopieer de **volledige URL** uit de adresbalk (begint met `https://ryklebaron.github.io/androidKmRegistratie/?code=...`)
5. Ga terug naar de app, plak de URL in het veld en tik **Bevestigen**
6. Bij succes zie je *Succesvol gekoppeld!* — de app vernieuwt het token voortaan automatisch

---

## Android Auto gebruiken

1. Sluit de telefoon aan op de auto (USB of draadloos)
2. Er verschijnt een melding op het vergrendelscherm: **Zakelijk / Privé / Doorgaan**
3. Kies het type rit — de rit wordt gestart en de km-stand opgehaald
4. Ontkoppel de telefoon om de rit te beëindigen

**Doorgaan met vorige rit**: als je binnen 60 seconden opnieuw verbindt (bijv. na een korte onderbreking), verschijnt de optie *Doorgaan*.

---

## Gegevens exporteren

Ga naar **Ritten** → tik het export-icoon rechtsboven → deel als CSV.

---

## Opnieuw koppelen

Het refresh-token heeft een beperkte geldigheidsduur. Als de km-stand niet meer opgehaald wordt, ga dan naar **Instellingen** en tik **Opnieuw koppelen** om de OAuth-flow opnieuw te doorlopen.
