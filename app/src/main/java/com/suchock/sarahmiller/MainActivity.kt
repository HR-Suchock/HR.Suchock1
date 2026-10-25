package com.suchock.sarahmiller

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.Context
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.TranslateAnimation
import android.widget.*
import java.util.Locale
import kotlin.math.max

private const val GOLD = 0xFFD6A85B.toInt()
private const val BG = 0xFF0A0C10.toInt()
private const val PANEL = 0xFF151922.toInt()
private const val PANEL2 = 0xFF202632.toInt()
private const val TEXT = 0xFFF1EEE7.toInt()
private const val MUTED = 0xFF9CA5B3.toInt()
private const val RED = 0xFFC95D5D.toInt()
private const val GREEN = 0xFF79B89A.toInt()

data class Choice(val label: String, val next: String, val clue: String? = null, val quest: String? = null)
data class Scene(val day: String, val place: String, val title: String, val speaker: String, val body: String, val choices: List<Choice>, val sideQuest: Boolean = false)

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val prefs by lazy { getSharedPreferences("sarah_deluxe_save", MODE_PRIVATE) }
    private val clues = linkedSetOf<String>()
    private val quests = linkedSetOf<String>()
    private val completedQuests = linkedSetOf<String>()
    private val messages = mutableListOf<String>()
    private var current = "intro"
    private var inGame = false
    private var soundOn = true
    private var voiceOn = true
    private var ambientOn = false
    private var textSize = 16f
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ambientPlayer: MediaPlayer? = null
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private var titleText = "SARAH MILLER CASE"
    private val scenes: Map<String, Scene> by lazy { makeScenes() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        soundOn = prefs.getBoolean("sound", true)
        voiceOn = prefs.getBoolean("voice", true)
        ambientOn = prefs.getBoolean("ambient", false)
        textSize = prefs.getFloat("textSize", 16f)
        current = prefs.getString("scene", "intro") ?: "intro"
        clues.addAll(prefs.getStringSet("clues", emptySet()) ?: emptySet())
        quests.addAll(prefs.getStringSet("quests", emptySet()) ?: emptySet())
        completedQuests.addAll(prefs.getStringSet("done", emptySet()) ?: emptySet())
        messages.addAll(
    prefs.getStringSet("messages", defaultMessages().toSet())
        ?: defaultMessages().toSet()
)
        showMenu()
    }

    override fun onInit(statusCode: Int) {
        ttsReady = statusCode == TextToSpeech.SUCCESS
        if (ttsReady) tts?.language = Locale.UK
    }

    override fun onDestroy() {
        ambientPlayer?.release()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun makeScenes(): Map<String, Scene> = listOf(
        Scene("CASE FILE 01", "ZERO POINT, DHAKA • 11:47 PM", "THE PHONE IN THE RAIN", "NARRATOR",
            "Rain scratches at the streetlights. Near the traffic island at Zero Point, a phone vibrates inside a torn evidence bag. Its lock screen shows one message: “If you found this, Sarah is already dead. Do not trust Hasan Rahman.”\n\nYour wife has called six times. You were meant to be home an hour ago. The phone has no SIM, but fresh mud marks its charging port.",
            listOf(Choice("Inspect the phone without touching the screen", "phone_clue", "Burner phone recovered"), Choice("Search the pavement for the person who dropped it", "street", "Wet tyre tracks"), Choice("Call your wife back first", "wife_call", "Unanswered calls"))),
        Scene("DAY 1", "ZERO POINT • EVIDENCE", "A MESSAGE WITH NO SENDER", "PHONE RECOVERY",
            "The device contains a damaged voice note and three fragments of an offline map. A notification names Priya, a journalist missing for two days. The voice note repeats one phrase: “The canal is not where they dumped her. It is where they moved the ledger.”",
            listOf(Choice("Recover metadata and map fragments", "metadata", "Map fragments"), Choice("Replay the damaged voice note", "voice", "Voice note: canal / ledger"), Choice("Open the in-game phone", "phone_hub"))),
        Scene("DAY 1", "RIVERSIDE ROAD", "THE BLACK SUV", "WITNESS: TEA SELLER",
            "A tea seller remembers a black SUV idling beneath a dead traffic camera. He saw a passenger with a bloodied cuff. He is afraid to say more while a motorbike circles the block. Tyre marks point toward the old riverside road.",
            listOf(Choice("Photograph the tyre tracks", "street", "SUV tyre pattern"), Choice("Protect the witness and listen", "street", "Witness: bloodied cuff", "witness"), Choice("Follow the motorbike", "canal", "Motorbike tail"))),
        Scene("DAY 1", "YOUR CAR • PHONE", "A VOICE IN THE BACKGROUND", "YOUR WIFE",
            "Your wife answers in a whisper. Behind her, a door slams and a man says, “The detective is still at Zero Point.” She insists she is fine, then hangs up. Someone knows where your family lives.",
            listOf(Choice("Send a trusted colleague to check on her", "phone_clue", "Threat at home", "family"), Choice("Keep her location private and continue", "phone_clue", "Threat at home"))),
        Scene("DAY 1", "DIGITAL FORENSICS", "THREE MINUTES ERASED", "FORENSICS",
            "The phone was wiped at 9:13 PM, but its offline map cache survives. One pin sits beside a disused warehouse owned by Rahman Holdings. Another marks the canal road. The third has been overwritten so aggressively that the storage chip is cracked.",
            listOf(Choice("Visit Warehouse 4 at first light", "warehouse", "Rahman warehouse pin"), Choice("Go to the canal tonight", "canal", "Canal route"), Choice("Check Priya's last assignment", "editor", "Priya's last assignment"))),
        Scene("DAY 1", "DIGITAL FORENSICS", "THE LAST TEN SECONDS", "RECOVERED AUDIO",
            "A second listen reveals a train horn, a metal gate, and a quiet voice counting: “Seven, two, nine.” The recording ends with a single impact. Is 729 a locker, a container, or a warning?",
            listOf(Choice("Cross-check the sound with the railway yard", "rail", "Audio: railway horn"), Choice("Search company records for 729", "records", "Number 729"))),
        Scene("DAY 2", "NEWSPAPER OFFICE", "PRIYA'S EMPTY DESK", "EDITOR",
            "Priya's editor reveals that Sarah Millar was investigating a procurement scandal tied to a private security contractor. A photograph shows Sarah standing beside the canal three days after the official time of death. Someone filed the death report in advance.",
            listOf(Choice("Ask who took the photograph", "photo", "Sarah alive after reported death"), Choice("Search Priya's desk", "desk", "Priya's source notebook"), Choice("Take the optional lead: find the missing camera card", "side_camera", "Camera-card lead", "camera_card"))),
        Scene("DAY 2", "RAHMAN HOLDINGS • WAREHOUSE 4", "THE SEALED ROOM", "NARRATOR",
            "The warehouse smells of diesel and wet concrete. A hidden door opens into an empty restraint room, a bloodied cufflink, and a wall map linking the station, the canal, and a clinic. Footsteps stop on the other side of the door.",
            listOf(Choice("Hide and photograph the conspiracy map", "warehouse_hide", "Conspiracy map"), Choice("Confront the guard", "guard", "Guard knows the name Rayan"), Choice("Slip out and find the clinic", "clinic", "Clinic marked on map"))),
        Scene("DAY 2", "WAREHOUSE 4 • HIDDEN ROOM", "THE SECOND NAME", "NARRATOR",
            "From behind the crates, you photograph the map. One pin is marked S.M. Another is marked R.K.—the initials of Rahman Kabir, the senior investigator who signed the original death report. A guard whispers into his phone: “Move the ledger before dawn.”",
            listOf(Choice("Escape with the photograph", "clinic", "R.K. signed death report"), Choice("Try to record the caller", "board", "Recorded call attempt"))),
        Scene("DAY 2", "WAREHOUSE • LOADING BAY", "A BAD TRADE", "GUARD",
            "The guard offers a name in exchange for the phone. You refuse. He lunges, but you escape with a split lip. The name he shouts is Rayan—Priya's inside source.",
            listOf(Choice("Track down Rayan", "rayan", "Name: Rayan"), Choice("Return to the evidence room", "board"))),
        Scene("DAY 2", "BURIGANGA CANAL ROAD • 2:16 AM", "THE WATER KEEPS SECRETS", "NARRATOR",
            "A work light swings over the canal. You find drag marks, a snapped camera mount, and a cigarette still burning in the rain. A figure bolts beneath the bridge. At the locked pump house, someone has scratched “729” into the door.",
            listOf(Choice("Enter the pump house", "pump", "Pump-house code"), Choice("Search the bridge for camera storage", "camera", "Broken camera mount"), Choice("Wait for backup", "rescue_plan", "Backup called"))),
        Scene("DAY 2", "RAILWAY GOODS YARD", "CONTAINER 729", "YARD CLERK",
            "Container 729 was leased by a Rahman subsidiary and opened twice after the official death report. The manifest lists medical equipment, but the recorded weight is wrong by nearly half a tonne.",
            listOf(Choice("Request the manifest officially", "records", "Container manifest"), Choice("Follow the payment trail", "records", "Paper trail"))),
        Scene("DAY 2", "PUBLIC RECORDS ROOM", "A COMPANY MADE OF PAPER", "NARRATOR",
            "A shell company was incorporated one day before Sarah vanished. Its director is dead. Payments link it to a clinic, a police evidence contractor, and a numbered account. The same account paid for a replacement camera at Zero Point—one that was never installed.",
            listOf(Choice("Follow the payments to the clinic", "clinic", "Shell-company payments"), Choice("Meet Rayan", "rayan", "Rayan lead"), Choice("Take the optional side case: missing witness", "side_witness", "Witness side-case", "witness_side"))),
        Scene("DAY 2", "NEWSPAPER ARCHIVE", "THE DATE THAT SHOULD NOT EXIST", "EDITOR",
            "The photograph's metadata places Sarah alive at 6:42 PM, after the death certificate was filed. In the canal reflection, a second person holds a silver umbrella. Priya believed the certificate was prepared before the abduction.",
            listOf(Choice("Identify the umbrella in company footage", "camera", "Silver umbrella reflection"), Choice("Find Priya's insider source", "rayan", "Priya's insider source"))),
        Scene("DAY 2", "PRIYA'S DESK", "INK UNDER THE PAPER", "NARRATOR",
            "A pressure mark remains on the page beneath Priya's missing notebook. You shade it with a pencil: “Rayan—clinic records. Don't trust the badge.” A tiny storage key is taped beneath the drawer.",
            listOf(Choice("Use the key at the clinic archive", "clinic", "Storage key"), Choice("Find Rayan first", "rayan", "Rayan's name"))),
        Scene("DAY 3", "PUMP HOUSE", "THE ROOM BELOW", "NARRATOR",
            "The code opens a service hatch. Beneath it is a dry chamber with a locked case and a bloodied scarf. The case contains blank ledger pages treated with a chemical that reveals writing under ultraviolet light. The pump starts by itself.",
            listOf(Choice("Take the case and escape", "clinic", "Invisible ledger"), Choice("Photograph the pages and leave the case", "clinic", "Invisible ledger photos"))),
        Scene("DAY 3", "TRAFFIC CAMERA DEPOT", "THE MISSING MINUTES", "TECHNICIAN",
            "The Zero Point feed was replaced with a loop. Original footage shows Sarah entering a vehicle voluntarily; the driver then turns toward the canal. The passenger wears a silver umbrella pin. The technician asks you not to put his name in the report.",
            listOf(Choice("Request the backup under warrant", "records", "Original camera frame"), Choice("Copy the drive quietly", "clinic", "Original camera footage"), Choice("Start optional side quest: identify the umbrella pin", "side_umbrella", "Umbrella-pin lead", "umbrella"))),
        Scene("DAY 3", "SHUTTERED CLINIC", "PATIENT ZERO", "NURSE",
            "The clinic archive lists Sarah under an alias. She was treated for a head wound, then transferred under police escort. A nurse remembers Rayan arguing with a man who wore a silver umbrella pin. In a locked cabinet is a list of payments to officers who altered the timeline.",
            listOf(Choice("Photograph the transfer records", "rayan", "Clinic transfer record"), Choice("Search the basement", "basement", "Basement access"), Choice("Call your wife and warn her", "wife_threat", "Family threatened"))),
        Scene("DAY 3", "SAFE FLAT", "RAYAN'S CONFESSION", "RAYAN",
            "Rayan opens the door holding a kitchen knife. He admits he drove Sarah away from the canal after she escaped the first abduction. He hid her at the clinic, but someone in the police chain found her. The ledger is the only proof that can survive a cover-up.",
            listOf(Choice("Offer witness protection and tell him the truth", "ledger", "Rayan testimony"), Choice("Pressure him for the name behind the false report", "rayan", "False death report lead"), Choice("Ask about Priya's missing recorder", "side_recorder", "Recorder lead", "recorder"))),
        Scene("DAY 3", "CLINIC BASEMENT", "THE EMPTY BED", "NARRATOR",
            "The basement holds restraints, a portable generator, and a fresh trail of disinfectant. A wall clock is stopped at 9:13—the same time the phone was wiped. You hear a lock click behind you.",
            listOf(Choice("Escape through the drainage tunnel", "ledger", "Drainage tunnel"), Choice("Stay hidden until footsteps pass", "board", "Basement escape"))),
        Scene("DAY 3", "PHONE CALL", "THE PRICE OF THE CASE", "YOUR WIFE",
            "Someone left a photograph of your family on the doorstep. Your wife wants you home. You promise you will come, but neither of you believes it. Protecting her may mean moving her out of the city.",
            listOf(Choice("Move her to a protected location", "ledger", "Family protection arranged"), Choice("Keep her home with trusted security", "ledger", "Family security arranged"))),
        Scene("DAY 4", "SAFE HOUSE", "THE LEDGER OF THE DEAD", "NARRATOR",
            "Under ultraviolet light, the blank pages reveal payments for forged death certificates, illegal transfers, intimidation, and a private detention operation. Sarah's name appears beside a transfer order signed by Hasan Rahman. The final entry names Kabir as the person who ordered Priya's disappearance.",
            listOf(Choice("Secure the ledger and make two copies", "priya", "Ledger decoded"), Choice("Confront Hasan with a decoy copy", "hasan", "Ledger decoded"), Choice("Leak selected pages to the press", "press", "Ledger leaked"))),
        Scene("DAY 4", "ABANDONED PRINT SHOP", "PRIYA IS ALIVE", "PRIYA",
            "A coded message leads to an abandoned print shop. Priya is alive, dehydrated, and furious. She says Hasan financed the operation but Kabir ran it. Sarah survived because she memorised offshore account numbers. Priya knows the compound where Sarah is held.",
            listOf(Choice("Get Priya medical help and plan a rescue", "rescue_plan", "Priya recovered"), Choice("Ask Priya to publish immediately", "press", "Priya's testimony"))),
        Scene("DAY 4", "RAHMAN HOLDINGS • EXECUTIVE FLOOR", "THE MAN IN THE GLASS ROOM", "HASAN RAHMAN",
            "Hasan claims he funded a security program to expose corruption, then lost control of it. He offers Sarah's location in exchange for the ledger. His office camera is recording, but its red light has been disconnected.",
            listOf(Choice("Pretend to accept and record his confession", "confession", "Hasan's statement"), Choice("Refuse the trade", "priya", "Hasan's offer"), Choice("Use a decoy ledger", "confession", "Decoy trade"))),
        Scene("DAY 4", "NEWSROOM", "A STORY THAT CAN GET PEOPLE KILLED", "EDITOR",
            "The newsroom can publish the financial trail tonight, but doing so may expose Sarah's location. The editor asks for proof that cannot be dismissed as fabricated. Priya insists the public deserves the truth now.",
            listOf(Choice("Publish the payments, hide rescue details", "rescue_plan", "Careful public leak"), Choice("Wait until Sarah is safe", "rescue_plan", "Publication delayed"))),
        Scene("DAY 4", "EXECUTIVE FLOOR", "A CONFESSION ON A DEAD CAMERA", "HASAN",
            "Hasan admits he authorised the transfers but says Kabir ordered the abductions. He reaches for a silent alarm. You have seconds to leave with the recording—or force him to reveal the compound location.",
            listOf(Choice("Escape with the recording", "rescue_plan", "Hasan confession recording"), Choice("Demand the compound location", "rescue_plan", "Compound location"))),
        Scene("DAY 5", "SAFE HOUSE • PRE-DAWN", "NO CLEAN RESCUE", "NARRATOR",
            "The compound is guarded by former officers. Priya can expose the gate schedule; Rayan knows a service tunnel. You have enough evidence to go public, but not enough time to wait for a warrant. One wrong move could get Sarah killed.",
            listOf(Choice("Enter through the service tunnel", "rescue_tunnel", "Service tunnel route"), Choice("Use a press contact to draw guards away", "rescue_tunnel", "Distraction plan"), Choice("Call an honest unit", "rescue_tunnel", "Police rescue plan"))),
        Scene("DAY 5", "PRIVATE COMPOUND • SERVICE TUNNEL", "THE DOOR UNDER THE EARTH", "SARAH",
            "The tunnel opens beneath a generator room. Sarah is chained to a pipe, weak but alert. She whispers: “Kabir isn't the top. Hasan is scared of someone above him.” An alarm begins to pulse red. Free Sarah, or secure the server cabinet containing the original records?",
            listOf(Choice("Free Sarah first", "escape", "Sarah rescued"), Choice("Copy the server records first", "escape", "Original server records"))),
        Scene("DAY 5", "COMPOUND • SERVICE ROAD", "THE LAST VEHICLE", "NARRATOR",
            "You escape with Sarah as the compound burns behind you. The fire may have been set to destroy the records. Sarah remembers account numbers, but the final proof is in a vault beneath the old courthouse. Kabir is on the run. Hasan has vanished.",
            listOf(Choice("Take Sarah to a secure hospital", "courthouse", "Sarah's account-number memory"), Choice("Go straight to the courthouse vault", "courthouse", "Vault lead"))),
        Scene("DAY 6", "EVIDENCE ROOM", "CONNECT THE RED THREAD", "NARRATOR",
            "The burner phone, false death report, shell-company payments, clinic transfer, and ledger point to Hasan as financier and Kabir as fixer. Sarah's memory suggests a third party missing from every official record.",
            listOf(Choice("Build a court-ready case", "courthouse", "Evidence board assembled"), Choice("Go to Kabir before he escapes", "kabir", "Evidence board assembled"), Choice("Follow the hidden account to the docks", "docks", "Third-party account"))),
        Scene("DAY 6", "POLICE HQ • ARCHIVE FLOOR", "THE MENTOR", "RAHMAN KABIR",
            "Kabir says he altered the timeline to keep Sarah alive, then lost control when Hasan demanded the ledger. He offers to testify if you erase his role in the cover-up. His gun lies on the table between you.",
            listOf(Choice("Reject the deal and record his full confession", "courthouse", "Kabir confession"), Choice("Accept a limited deal", "courthouse", "Kabir testimony"), Choice("Start the optional side quest: recover the evidence-room key", "side_key", "Evidence-room key lead", "key_quest"))),
        Scene("DAY 6", "OLD COURTHOUSE • VAULT", "THE FINAL KEY", "NARRATOR",
            "The vault key is hidden inside a cracked memorial plaque. Records prove the payments and transfers, but the original authorisation is missing. A handwritten note says it was sent to a private dock under the name “Morrow.”",
            listOf(Choice("Take records to an independent prosecutor", "ending_good", "Vault records"), Choice("Go to the docks for the original authorisation", "docks", "Morrow note"), Choice("Publish everything immediately", "ending_public", "Vault records"))),
        Scene("DAY 6", "OLD DOCKS • 4:38 AM", "THE PERSON ABOVE HASAN", "UNKNOWN CALLER",
            "A container is open beside the water. Inside is the original authorisation, bearing the seal of a private intelligence broker known as Morrow. A phone rings. “You solved the case you were given. Don't mistake it for the case that happened.” Headlights sweep across the dock.",
            listOf(Choice("Secure the file and go to the prosecutor", "ending_good", "Original authorisation"), Choice("Confront the unseen buyer", "ending_truth", "Morrow identity clue"), Choice("Destroy the file and protect Sarah", "ending_protect", "File destroyed"))),
        Scene("SIDE CASE", "OLD CINEMA • BACK ROOM", "THE MISSING CAMERA CARD", "SIDE QUEST",
            "A projectionist kept a camera card found near Zero Point. It contains seven seconds of footage: a silver umbrella pin, a vehicle door, and a hand passing a sealed envelope. The face is obscured, but the time stamp proves the official timeline was falsified.",
            listOf(Choice("Log the card as evidence and return to the case", "editor", "Camera card recovered", "camera_card_done")) , true),
        Scene("SIDE CASE", "BUS DEPOT • LOCKER ROW", "THE WITNESS WHO VANISHED", "SIDE QUEST",
            "A bus conductor saw a second vehicle leaving the canal. He has been hiding in a depot locker room. He gives you a partial plate and asks for safe passage out of Dhaka. Protecting him could turn suspicion into testimony.",
            listOf(Choice("Arrange a safe transfer", "records", "Witness secured", "witness_done"), Choice("Take the plate and leave", "records", "Partial plate")) , true),
        Scene("SIDE CASE", "ROOFTOP • RAINWATER TANK", "THE SILVER UMBRELLA", "SIDE QUEST",
            "A pawn-shop receipt traces the umbrella pin to a private security club. The club ledger lists a payment to an account called MORROW. You now have a link between the street-level operation and the hidden buyer.",
            listOf(Choice("Photograph the ledger and return", "camera", "Morrow account link", "umbrella_done")) , true),
        Scene("SIDE CASE", "ABANDONED RADIO BOOTH", "PRIYA'S LAST RECORDING", "SIDE QUEST",
            "A tiny recorder is hidden behind a loose panel. Priya's voice says she was followed by a man who used the phrase “blue room”. In the background, a station announcement names the freight yard. The recording could prove she was held before the public search began.",
            listOf(Choice("Keep the recording safe and return", "rayan", "Priya recording recovered", "recorder_done")) , true),
        Scene("SIDE CASE", "EVIDENCE ROOM • AFTER HOURS", "THE KEY THAT SHOULDN'T EXIST", "SIDE QUEST",
            "A duplicate key opens a sealed evidence drawer. Inside is a copy of the original death report, timestamped before Sarah disappeared. The signature is genuine. The order came from above Kabir's rank.",
            listOf(Choice("Photograph the report and return", "kabir", "Pre-dated death report", "key_done")) , true),
        Scene("CASE CLOSED?", "DHAKA • MORNING", "JUSTICE, WITH A CRACK IN IT", "NARRATOR",
            "The independent prosecutor receives the ledger, camera backup, payment trail, and original authorisation. Hasan is arrested before sunrise. Kabir's confession brings down the compromised unit. Sarah survives, Priya publishes the investigation, and the witness is relocated.\n\nBut the final file carries a second seal: MORROW. Someone higher than Hasan funded the operation—and your name is already in their next case file.\n\nENDING: THE CASE IS PROVEN. THE CONSPIRACY IS NOT OVER.",
            listOf(Choice("Return to title screen", "intro"))),
        Scene("CASE CLOSED?", "NEWSROOM • MORNING", "THE TRUTH GOES LIVE", "NARRATOR",
            "The story breaks nationwide. Hasan's empire collapses, and the public sees the ledger. But the rushed leak exposes a witness and gives the network time to destroy the original authorisation. Sarah is alive, yet the strongest charges may not survive court.\n\nENDING: PUBLIC TRUTH, INCOMPLETE JUSTICE.",
            listOf(Choice("Return to title screen", "intro"))),
        Scene("CASE CLOSED?", "BORDER ROAD • DAWN", "ONE LIFE SAVED", "NARRATOR",
            "You destroy the file and get Sarah out. For the first time in days, there is no siren behind you. Hasan remains untouchable in public, but Priya keeps a copy of the truth. The phone buzzes: “Thank you for choosing the living. —Morrow.”\n\nENDING: SARAH LIVES. THE SYSTEM SURVIVES.",
            listOf(Choice("Return to title screen", "intro"))),
        Scene("CASE CLOSED?", "OLD DOCKS", "MORROW", "UNKNOWN CALLER",
            "The headlights reveal no face, only a driver wearing a silver umbrella pin. A suppressed shot shatters the phone. When you wake, the authorisation is gone and a new case file lies beside you—your own name on the cover.\n\nENDING: THE NEXT TARGET.",
            listOf(Choice("Return to title screen", "intro")))
    ).associateBy { scene -> when(scene.title) {
        "THE PHONE IN THE RAIN" -> "intro"; "A MESSAGE WITH NO SENDER" -> "phone_clue"; "THE BLACK SUV" -> "street"; "A VOICE IN THE BACKGROUND" -> "wife_call"
        "THREE MINUTES ERASED" -> "metadata"; "THE LAST TEN SECONDS" -> "voice"; "PRIYA'S EMPTY DESK" -> "editor"; "THE SEALED ROOM" -> "warehouse"
        "THE SECOND NAME" -> "warehouse_hide"; "A BAD TRADE" -> "guard"; "THE WATER KEEPS SECRETS" -> "canal"; "CONTAINER 729" -> "rail"
        "A COMPANY MADE OF PAPER" -> "records"; "THE DATE THAT SHOULD NOT EXIST" -> "photo"; "INK UNDER THE PAPER" -> "desk"; "THE ROOM BELOW" -> "pump"
        "THE MISSING MINUTES" -> "camera"; "PATIENT ZERO" -> "clinic"; "RAYAN'S CONFESSION" -> "rayan"; "THE EMPTY BED" -> "basement"; "THE PRICE OF THE CASE" -> "wife_threat"
        "THE LEDGER OF THE DEAD" -> "ledger"; "PRIYA IS ALIVE" -> "priya"; "THE MAN IN THE GLASS ROOM" -> "hasan"; "A STORY THAT CAN GET PEOPLE KILLED" -> "press"
        "A CONFESSION ON A DEAD CAMERA" -> "confession"; "NO CLEAN RESCUE" -> "rescue_plan"; "THE DOOR UNDER THE EARTH" -> "rescue_tunnel"; "THE LAST VEHICLE" -> "escape"
        "CONNECT THE RED THREAD" -> "board"; "THE MENTOR" -> "kabir"; "THE FINAL KEY" -> "courthouse"; "THE PERSON ABOVE HASAN" -> "docks"
        "THE MISSING CAMERA CARD" -> "side_camera"; "THE WITNESS WHO VANISHED" -> "side_witness"; "THE SILVER UMBRELLA" -> "side_umbrella"
        "PRIYA'S LAST RECORDING" -> "side_recorder"; "THE KEY THAT SHOULDN'T EXIST" -> "side_key"; "JUSTICE, WITH A CRACK IN IT" -> "ending_good"
        "THE TRUTH GOES LIVE" -> "ending_public"; "ONE LIFE SAVED" -> "ending_protect"; "MORROW" -> "ending_truth"; else -> scene.title.lowercase().replace(" ", "_")
    } }

    private fun showMenu() {
        inGame = false
        buildBase()
        val hero = TextView(this).apply {
            text = "S A R A H\nM I L L E R\n\nC A S E"
            textSize = 35f
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            setTextColor(TEXT)
            gravity = Gravity.CENTER
            setPadding(0, dp(38), 0, dp(12))
            letterSpacing = 0.08f
        }
        content.addView(hero)
        content.addView(label("A CINEMATIC DETECTIVE THRILLER  /  DHAKA"))
        val line = View(this).apply { setBackgroundColor(GOLD) }
        content.addView(line, LinearLayout.LayoutParams(dp(70), dp(2)).apply { gravity = Gravity.CENTER; topMargin = dp(18); bottomMargin = dp(24) })
        content.addView(actionButton("▶   NEW GAME", true) { newGame() }, matchParams(56, 12))
        content.addView(actionButton("▣   CONTINUE CASE", false) { if (prefs.getBoolean("hasSave", false)) { current = prefs.getString("scene","intro") ?: "intro"; showGame() } else toast("No saved case yet. Start a new game.") }, matchParams(52, 8))
        content.addView(actionButton("⚙   SETTINGS", false) { showSettings(false) }, matchParams(52, 8))
        content.addView(actionButton("CASE ARCHIVE  /  EVIDENCE", false) { showArchive() }, matchParams(52, 8))
        content.addView(label("\nOFFLINE STORY • CHOICE-DRIVEN • VOICE READOUT • SIDE CASES"))
        content.addView(label("Build 2.0  /  All progress stored on this device"))
        fade(content)
    }

    private fun newGame() {
        current = "intro"; clues.clear(); quests.clear(); completedQuests.clear()
        messages.clear(); messages.addAll(defaultMessages())
        save()
        showGame()
    }

    private fun buildBase() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BG) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), dp(14), dp(18), dp(10)); setBackgroundColor(0xFF0D1016.toInt()) }
        top.addView(TextView(this).apply { text = "S.M.  /  CASE FILES"; textSize = 12f; letterSpacing = .08f; typeface = Typeface.MONOSPACE; setTextColor(GOLD) }, LinearLayout.LayoutParams(0,-2,1f))
        top.addView(smallButton("☰") { showPhoneHub() })
        top.addView(smallButton("⚙") { showSettings(inGame) })
        root.addView(top)
        status = TextView(this).apply { textSize = 10f; typeface = Typeface.MONOSPACE; setTextColor(MUTED); setPadding(dp(18), dp(10), dp(18), dp(10)); setBackgroundColor(0xFF10141B.toInt()) }
        root.addView(status)
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(28)) }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(6), dp(8), dp(6), dp(8)); setBackgroundColor(0xFF0D1016.toInt()) }
        listOf("CASE","PHONE","JOURNAL","QUESTS").forEach { item -> nav.addView(smallButton(item) { when(item) { "CASE" -> if(inGame) showGame() else showMenu(); "PHONE" -> showPhoneHub(); "JOURNAL" -> showJournal(); else -> showQuests() } }, LinearLayout.LayoutParams(0, dp(44), 1f)) }
        root.addView(nav)
        setContentView(root)
    }

    private fun showGame() {
        inGame = true
        val s = scenes[current] ?: scenes["intro"]!!
        buildBase()
        status.text = "${s.day}   •   ${s.place}   •   CLUES ${clues.size}"
        content.addView(label("FIELD REPORT  /  ${s.speaker}"))
        content.addView(title(s.title))
        val sceneCard = panel()
        sceneCard.addView(TextView(this).apply {
            text = s.body; textSize = textSize; setTextColor(TEXT); setLineSpacing(dp(5).toFloat(), 1f)
        })
        content.addView(sceneCard, matchParams(-2, 12))
        if (s.sideQuest) content.addView(label("OPTIONAL SIDE CASE  •  REWARD: EVIDENCE / TRUST"))
        content.addView(label("YOUR NEXT MOVE"))
        s.choices.forEachIndexed { i, choice ->
            content.addView(actionButton("${(i+1).toString().padStart(2,'0')}   ${choice.label}", false) {
                choice.clue?.let { clues.add(it) }
                choice.quest?.let { if (it.endsWith("_done")) completedQuests.add(it.removeSuffix("_done")) else quests.add(it) }
                current = choice.next
                if (current == "phone_hub") { save(); showPhoneHub() } else { save(); showGame() }
            }, matchParams(-2, 8))
        }
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(smallButton("▶ READ ALOUD") { speak(s.body) }, LinearLayout.LayoutParams(0, dp(46), 1f))
        actions.addView(smallButton("▣ SAVE") { save(); toast("Case saved") }, LinearLayout.LayoutParams(0, dp(46), 1f))
        actions.addView(smallButton("EVIDENCE") { showArchive() }, LinearLayout.LayoutParams(0, dp(46), 1f))
        content.addView(actions, matchParams(-2, 14))
        fade(content)
    }

    private fun showPhoneHub() {
        inGame = true
        buildBase()
        status.text = "IN-GAME DEVICE   •   OFFLINE MODE   •   BATTERY 78%"
        content.addView(title("FIELD PHONE"))
        content.addView(label("A secured handset issued to the case team. Apps are simulated in-game; they do not access your real phone data."))
        val apps = listOf(
            Triple("MESSAGES", "Read incoming texts and case contacts", { showMessages() }),
            Triple("CALLS", "Call contacts (story dialogue)", { showCalls() }),
            Triple("CAMERA", "Review collected photo evidence", { showArchive() }),
            Triple("MAP", "Open known investigation locations", { showMap() }),
            Triple("NOTES", "Read detective notebook", { showJournal() }),
            Triple("VOICE RECORDER", "Replay a recovered recording", { speak("Recovered audio. Seven. Two. Nine. The canal is not where they dumped her. It is where they moved the ledger.") }),
            Triple("CASE DATABASE", "People, suspects and evidence", { showArchive() }),
            Triple("SETTINGS", "Sound, voice and text options", { showSettings(true) })
        )
        val grid = GridLayout(this).apply { columnCount = 2; useDefaultMargins = true }
        apps.forEach { app ->
            val tile = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(10), dp(18), dp(10), dp(18)); background = rounded(PANEL2, 14) }
            tile.addView(TextView(this).apply { text = app.first; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setTextColor(GOLD) })
            tile.addView(TextView(this).apply { text = app.second; textSize = 10f; gravity = Gravity.CENTER; setTextColor(MUTED); setPadding(0,dp(8),0,0) })
            tile.setOnClickListener { app.third() }
            grid.addView(tile, GridLayout.LayoutParams().apply { width = 0; height = dp(112); columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); setMargins(dp(4),dp(4),dp(4),dp(4)) })
        }
        content.addView(grid)
        content.addView(actionButton("RETURN TO INVESTIGATION", true) { showGame() }, matchParams(52, 14))
        fade(content)
    }

    private fun showMessages() {
        buildBase(); status.text = "PHONE / MESSAGES"
        content.addView(title("MESSAGES"))
        messages.forEachIndexed { i, msg ->
            val card = panel()
            card.addView(label(if(i==0) "UNKNOWN NUMBER • 11:49 PM" else "CASE CONTACT • ${11+i}:0${i%6} PM"))
            card.addView(TextView(this).apply { text = msg; textSize = 15f; setTextColor(TEXT); setPadding(0,dp(7),0,0) })
            content.addView(card, matchParams(-2, 8))
        }
        content.addView(actionButton("REPLY: ASK WHO SENT THE PHONE", false) { messages.add("You: Who sent the phone?"); messages.add("Unknown: Ask the man who signed the death report."); save(); showMessages() }, matchParams(48, 8))
        content.addView(actionButton("BACK TO PHONE", true) { showPhoneHub() }, matchParams(48, 8))
    }

    private fun showCalls() {
        buildBase(); status.text = "PHONE / CALLS"
        content.addView(title("CONTACTS"))
        listOf("Priya Sen  /  Journalist", "Rayan  /  Confidential source", "Case partner  /  Police", "Home  /  Family", "Unknown caller  /  Restricted").forEach { contact ->
            content.addView(actionButton("☎   $contact", false) {
                val line = when {
                    contact.startsWith("Priya") -> "Priya: If you are hearing this, the phone reached you. Don't trust the badge. Find the clinic transfer log."
                    contact.startsWith("Rayan") -> "Rayan: I can tell you where Sarah was moved, but you need to prove you can protect the witness."
                    contact.startsWith("Case") -> "Partner: The station is leaking. Keep a copy of every clue outside the official system."
                    contact.startsWith("Home") -> "Your wife: I heard someone outside again. Please tell me you're coming home."
                    else -> "Unknown caller: You have the wrong case file, detective. Stop before the docks."
                }
                dialog(contact.substringBefore(" / "), line)
            }, matchParams(52, 7))
        }
        content.addView(actionButton("BACK TO PHONE", true) { showPhoneHub() }, matchParams(48, 12))
    }

    private fun showMap() {
        buildBase(); status.text = "PHONE / OFFLINE MAP"
        content.addView(title("CASE MAP"))
        val locs = listOf("01  ZERO POINT  •  Phone recovered", "02  WAREHOUSE 4  •  Rahman Holdings", "03  BURIGANGA CANAL  •  Camera missing", "04  SHUTTERED CLINIC  •  Transfer records", "05  SAFE FLAT  •  Rayan's testimony", "06  OLD COURTHOUSE  •  Vault", "07  OLD DOCKS  •  Morrow")
        locs.forEachIndexed { i, loc -> content.addView(actionButton("⌖   $loc", false) { dialog("MAP MARKER ${i+1}", "Location marked in the offline case map. Travel details unlock as the investigation progresses.") }, matchParams(48, 7)) }
        content.addView(actionButton("BACK TO PHONE", true) { showPhoneHub() }, matchParams(48, 12))
    }

    private fun showJournal() {
        buildBase(); status.text = "DETECTIVE JOURNAL"
        content.addView(title("FIELD NOTES"))
        val notes = listOf("Victim: Sarah Millar — official death time disputed.", "Persons of interest: Hasan Rahman, Rahman Kabir, Rayan, Priya Sen, unknown broker Morrow.", "Recurring evidence: burner phone wiped at 9:13 PM; number 729; silver umbrella pin; black SUV.", "Working theory: false death report enabled a covert transfer operation.", "Rule: keep duplicate evidence and protect witnesses.")
        notes.forEach { note -> content.addView(panel().apply { addView(TextView(this@MainActivity).apply { text = "—  $note"; textSize = 15f; setTextColor(TEXT) }) }, matchParams(-2, 7)) }
        content.addView(actionButton("OPEN EVIDENCE BOARD", true) { showArchive() }, matchParams(48, 10))
    }

    private fun showArchive() {
        buildBase(); status.text = "CASE DATABASE  /  ${clues.size} CLUES"
        content.addView(title("EVIDENCE BOARD"))
        content.addView(label("CLUES COLLECTED"))
        if (clues.isEmpty()) content.addView(label("No clues collected yet. Investigate scenes and interview contacts."))
        clues.forEach { clue -> content.addView(panel().apply { addView(TextView(this@MainActivity).apply { text = "◆  $clue"; textSize = 14f; setTextColor(TEXT) }) }, matchParams(-2, 6)) }
        content.addView(label("\nPEOPLE OF INTEREST"))
        listOf("Sarah Millar — missing investigator / presumed dead", "Priya Sen — journalist / recovered source", "Rayan — insider / witness", "Hasan Rahman — financier", "Rahman Kabir — senior investigator", "Morrow — unidentified broker").forEach { person ->
            content.addView(actionButton(person, false) { dialog(person.substringBefore(" — "), person) }, matchParams(44, 5))
        }
        content.addView(actionButton("BACK TO CASE", true) { if(inGame) showGame() else showMenu() }, matchParams(48, 12))
    }

    private fun showQuests() {
        buildBase(); status.text = "OPTIONAL INVESTIGATIONS"
        content.addView(title("SIDE CASES"))
        val questList = listOf("camera_card" to "The Missing Camera Card", "witness_side" to "The Witness Who Vanished", "umbrella" to "The Silver Umbrella", "recorder" to "Priya's Last Recording", "key_quest" to "The Key That Shouldn't Exist")
        questList.forEach { (id,name) ->
            val done = completedQuests.contains(id+"_done") || completedQuests.contains(id)
            val started = quests.contains(id)
            val line = if(done) "COMPLETED" else if(started) "IN PROGRESS" else "NOT STARTED"
            content.addView(panel().apply {
                addView(TextView(this@MainActivity).apply { text = name; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setTextColor(TEXT) })
                addView(TextView(this@MainActivity).apply { text = line; textSize = 11f; setTextColor(if(done) GREEN else GOLD); setPadding(0,dp(5),0,0) })
            }, matchParams(-2, 7))
        }
        content.addView(actionButton("INVESTIGATE AVAILABLE SIDE CASE", true) {
            val target = when { !completedQuests.contains("camera_card_done") -> "side_camera"; !completedQuests.contains("witness_done") -> "side_witness"; !completedQuests.contains("umbrella_done") -> "side_umbrella"; !completedQuests.contains("recorder_done") -> "side_recorder"; else -> "side_key" }
            current = target; quests.add(target.removePrefix("side_")); save(); showGame()
        }, matchParams(52, 10))
        content.addView(actionButton("BACK TO CASE", false) { showGame() }, matchParams(48, 5))
    }

    private fun showSettings(fromGame: Boolean) {
        buildBase(); status.text = "SYSTEM SETTINGS"
        content.addView(title("SETTINGS"))
        content.addView(label("AUDIO"))
        content.addView(actionButton("VOICE DIALOGUE  :  ${if(voiceOn) "ON" else "OFF"}", false) { voiceOn = !voiceOn; prefs.edit().putBoolean("voice",voiceOn).apply(); showSettings(fromGame) }, matchParams(50, 7))
        content.addView(actionButton("SOUND EFFECTS  :  ${if(soundOn) "ON" else "OFF"}", false) { soundOn = !soundOn; prefs.edit().putBoolean("sound",soundOn).apply(); if(soundOn) clickSound(); showSettings(fromGame) }, matchParams(50, 7))
        content.addView(actionButton("AMBIENT RAIN  :  ${if(ambientOn) "ON" else "OFF"}", false) { ambientOn = !ambientOn; prefs.edit().putBoolean("ambient",ambientOn).apply(); toast(if(ambientOn) "Ambient preference enabled. Add a looped rain track to expand ambience." else "Ambient preference disabled."); showSettings(fromGame) }, matchParams(50, 7))
        content.addView(label("\nACCESSIBILITY"))
        content.addView(actionButton("TEXT SIZE  :  ${textSize.toInt()} SP", false) { textSize = if(textSize >= 20f) 14f else textSize + 2f; prefs.edit().putFloat("textSize",textSize).apply(); showSettings(fromGame) }, matchParams(50, 7))
        content.addView(label("\nGAME"))
        content.addView(actionButton("SAVE CURRENT CASE", false) { save(); toast("Saved locally") }, matchParams(50, 7))
        content.addView(actionButton("RESET ALL PROGRESS", false) { android.app.AlertDialog.Builder(this).setTitle("Reset case?").setMessage("This deletes local game progress and clues.").setNegativeButton("Cancel",null).setPositiveButton("Reset") { _,_ -> newGame() }.show() }, matchParams(50, 7))
        content.addView(actionButton("BACK", true) { if(fromGame) showGame() else showMenu() }, matchParams(50, 12))
    }

    private fun dialog(who: String, line: String) {
        android.app.AlertDialog.Builder(this).setTitle(who).setMessage(line)
            .setPositiveButton("READ ALOUD") { _,_ -> speak(line) }
            .setNegativeButton("CLOSE", null).show()
    }

    private fun speak(text: String) {
        if (!voiceOn) { toast("Voice dialogue is disabled in Settings."); return }
        if (!ttsReady) { toast("Android text-to-speech is still starting. Try again."); return }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "case_${System.currentTimeMillis()}")
    }

    private fun clickSound() {
        if (!soundOn) return
        try {
            val tone = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 35)
            tone.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 80)
            android.os.Handler(mainLooper).postDelayed({ tone.release() }, 180)
        } catch (_: Exception) {}
    }

    private fun save() {
        prefs.edit().putString("scene",current).putStringSet("clues",HashSet(clues))
            .putStringSet("quests",HashSet(quests)).putStringSet("done",HashSet(completedQuests))
            .putStringSet("messages",HashSet(messages)).putBoolean("hasSave",true)
            .putBoolean("sound",soundOn).putBoolean("voice",voiceOn).putBoolean("ambient",ambientOn)
            .putFloat("textSize",textSize).apply()
    }

    private fun defaultMessages() = listOf(
        "Unknown: If you found this phone, Sarah is already dead. Do not trust Hasan Rahman.",
        "Priya: The canal is not where they dumped her. It is where they moved the ledger.",
        "Unknown: 729. Listen for the train horn. Do not go to the station alone."
    )

    private fun panel(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15),dp(14),dp(15),dp(14))
        background = rounded(PANEL, 14)
    }

    private fun title(t: String) = TextView(this).apply {
        text = t; textSize = 27f; typeface = Typeface.create("sans-serif-condensed",Typeface.BOLD)
        setTextColor(TEXT); letterSpacing = .01f; setPadding(0,dp(8),0,dp(12))
    }

    private fun label(t: String) = TextView(this).apply {
        text = t; textSize = 10f; typeface = Typeface.MONOSPACE; letterSpacing = .05f
        setTextColor(GOLD); setPadding(0,dp(5),0,dp(9))
    }

    private fun actionButton(t: String, primary: Boolean, action: () -> Unit) = Button(this).apply {
        text = t; isAllCaps = false; textSize = 13f; gravity = Gravity.CENTER_VERTICAL
        setTextColor(if(primary) BG else TEXT)
        background = rounded(if(primary) GOLD else PANEL2, 12)
        setPadding(dp(14),dp(5),dp(14),dp(5))
        setOnClickListener { clickSound(); action() }
    }

    private fun smallButton(t: String, action: () -> Unit) = Button(this).apply {
        text = t; isAllCaps = false; textSize = 10f; setTextColor(GOLD)
        background = rounded(PANEL2, 10); minHeight = dp(38); minWidth = dp(44)
        setPadding(dp(5),0,dp(5),0); setOnClickListener { clickSound(); action() }
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), if(color == GOLD) GOLD else 0xFF303746.toInt())
    }

    private fun matchParams(height: Int, top: Int) = LinearLayout.LayoutParams(-1, if(height == -2) -2 else dp(height)).apply { topMargin = dp(top) }
    private fun fade(v: View) { v.startAnimation(AlphaAnimation(0.0f,1.0f).apply { duration = 240 }) }
    private fun toast(s: String) = Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
}
