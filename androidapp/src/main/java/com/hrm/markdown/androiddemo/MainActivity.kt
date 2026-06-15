package com.hrm.markdown.androiddemo

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.markdown.App
import com.hrm.markdown.MathMarkdown
import com.hrm.markdown.parser.log.HLog
import com.hrm.markdown.parser.log.ILogger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        HLog.setLogger(object : ILogger {
            override fun v(tag: String, message: String) {
                Log.v(tag, message)
            }

            override fun d(tag: String, message: String) {
                Log.d(tag, message)
            }

            override fun i(tag: String, message: String) {
                Log.i(tag, message)
            }

            override fun w(tag: String, message: String) {
                Log.w(tag, message)
            }

            override fun e(
                tag: String,
                message: String,
                throwable: Throwable?
            ) {
                Log.e(tag, message, throwable)
            }
        })

        setContent {
//            App()
            QuizScreenPreview(topic = "electricity")
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}



private val json = Json { ignoreUnknownKeys = true }


fun getQuizFile(context: Context, topic: String, hexKey: String): QuizFile? {
    val assetName = GRADE_10_ASSET_MAP[topic.trim().lowercase()] ?: return null
    return try {
        val plaintext = EncryptedAssetReader.decryptToString(context, assetName, hexKey)
        json.decodeFromString<QuizFile>(plaintext)
    } catch (e: Exception) {
        Log.d("QUIZ FILE ERROR",e.message.toString())
        e.printStackTrace()
        null
    }
}


object EncryptedAssetReader {
    fun decryptToString(context: Context, assetName: String, hexKey: String): String {
        val keyBytes = hexKey.trim().lowercase().chunked(2)
            .map { it.toInt(16).toByte() }.toByteArray()

        val raw = context.assets.open(assetName).use { it.readBytes() }

        val iv         = raw.copyOfRange(0, 12)
        val tag        = raw.copyOfRange(12, 28)
        val ciphertext = raw.copyOfRange(28, raw.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(keyBytes, "AES"),
            GCMParameterSpec(128, iv)
        )
        return cipher.doFinal(ciphertext + tag).toString(Charsets.UTF_8)
    }
}

val GRADE_10_ASSET_MAP: Map<String, String> = mapOf(
    "chemical bonding"                        to "grade-10-physics-chemical-bonding.json.enc",
    "classification of matter"                to "grade-10-physics-classification-of-matter.json.enc",
    "electricity"                             to "testtttt.json.enc",
    "electrostatics"                          to "grade-10-physics-electrostatics.json.enc",
    "energy"                                  to "grade-10-physics-energy.json.enc",
    "hydrosphere"                             to "grade-10-physics-hydrosphere.json.enc",
    "kinetic theory of matter"                to "grade-10-physics-kinetic-theory-of-matter.json.enc",
    "magnetism"                               to "grade-10-physics-magnetism.json.enc",
    "motion in 1d"                            to "grade-10-physics-motion-in-1d.json.enc",
    "periodic table"                          to "grade-10-physics-the-periodic-table.json.enc",
    "quantitative aspects of chemical change" to "grade-10-physics-quantitative-aspects-of-chemical-change.json.enc",
    "radiation"                               to "grade-10-physics-radiation.json.enc",
    "reactions"                               to "grade-10-physics-reactions.json.enc",
    "sound"                                   to "grade-10-physics-sound.json.enc",
    "the atom"                                to "grade-10-physics-the-atom.json.enc",
    "vectors & scalars"                       to "grade-10-physics-vectors-and-scalors.json.enc",  // note: typo in filename preserved
    "vectors and scalars"                     to "grade-10-physics-vectors-and-scalors.json.enc",  // alias
    "waves"                                   to "grade-10-physics-waves.json.enc",
)



@Composable
fun QuizScreen(
    quizFile: QuizFile,
    currentUser: UserModel?,
    myRank: Int,
    onQuizFinished: (
        correctCount: Int,
        total: Int,
        totalPoints: Int,
        answers: List<AnsweredQuestion>
    ) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val listOfQuestions = remember { quizFile.questions.shuffled().take(30) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var correctCount       by remember { mutableIntStateOf(0) }
    var totalPoints        by remember { mutableIntStateOf(0) }
    var answered           by remember { mutableStateOf(false) }
    var selectedAnswerId   by remember { mutableStateOf<String?>(null) }
    val answeredQuestions  = remember { mutableStateListOf<AnsweredQuestion>() }

    val currentQuestion = listOfQuestions.getOrNull(currentQuestionIndex)

    if (currentQuestion == null) {
        LaunchedEffect(Unit) {
            onQuizFinished(correctCount, listOfQuestions.size, totalPoints, answeredQuestions.toList())
        }
        return
    }

    Scaffold(
        topBar = {
            QuizTopBar(
                topic       = quizFile.topic,
                current     = currentQuestionIndex + 1,
                total       = listOfQuestions.size,
                totalPoints = totalPoints,
                myRank      = myRank,
                currentUser = currentUser
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize().background(Color.White)
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {

            AnimatedContent(
                targetState = currentQuestionIndex,
                transitionSpec = {
                    fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                },
                label = "quizTransition"
            ) { index ->

                val question = (listOfQuestions[index])
                Log.d("CURRENT QUESTION",question.toString())

                Column(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth().background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // ── Question counter pill ──
                    QuestionCounterPill(
                        index = index,
                        total = listOfQuestions.size
                    )

                    QuestionBlock(text = question.question)

                    question.answers.forEach { answer ->
                        AnswerOptionButton(
                            buttonText = answer.text,
                            isSelected = answer.id == selectedAnswerId,
                            isCorrect  = answered && answer.id == question.correct,
                            isWrong    = answered && answer.id == selectedAnswerId && answer.id != question.correct,
                            onClick = {
                                if (!answered) {
                                    selectedAnswerId = answer.id
                                    answered = true

                                    val correct = answer.id == question.correct
                                    val pts = if (correct) 10 else 0

                                    if (correct) correctCount++

//                                    if (correct){
//                                        successVibrate(context)
//                                    }else{
//                                        failedVibrate(context)
//                                    }


                                    totalPoints += pts

                                    answeredQuestions.add(
                                        AnsweredQuestion(
                                            question = question,
                                            selectedId = answer.id,
                                            isCorrect = correct,
                                            pointsEarned = pts
                                        )
                                    )
                                }
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = answered,
                        enter = fadeIn(tween(200)) + expandVertically(),
                        exit  = fadeOut(tween(150)) + shrinkVertically()
                    ) {
                        ExplanationBlock(text = question.explanation)
                    }

                    ContinueButton(
                        enabled = answered,
                        onClick = {
                            answered = false
                            selectedAnswerId = null
                            currentQuestionIndex++
                        }
                    )
                }
            }
        }
    }


}

@Composable
fun QuizTopBar(
    topic: String,
    current: Int,
    total: Int,
    totalPoints: Int,
    myRank: Int,
    currentUser: UserModel?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {

                // Topic
                Text(
                    text       = topic,

                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 18.sp,
                    color      = Color.Black,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(5.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Rank badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black
                    ) {
                        Text(
                            text     = "Rank #${myRank + 1}",
                            color    = Color.White,

                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }

                    // Points earned
                    Surface(
                        shape  = RoundedCornerShape(50),
                        color  = Color(0xFFF5F5F5),
                        border = BorderStroke(1.dp, Color(0xFFE5E5E5))
                    ) {
                        Row(
                            modifier              = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
//                            Icon(
//                                painter            = painterResource(R.drawable.trophy_duotone), // your drawable
//                                contentDescription = null,
//                                tint               = Color(0xFFF5A623),
//                                modifier           = Modifier.size(12.dp)
//                            )
                            Text(
                                text       = "$totalPoints pts",

                                fontWeight = FontWeight.Bold,
                                fontSize   = 11.sp,
                                color      = Color.Black
                            )
                        }
                    }
                }
            }

            // User avatar
//            AsyncImage(
//                model          = currentUser?.userImage,
//                contentDescription = null,
//                contentScale   = ContentScale.Crop,
//                modifier       = Modifier
//                    .size(50.dp)
//                    .clip(CircleShape)
//                    .border(2.dp, Color.Black, CircleShape)
//            )
        }

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color(0xFFEEEEEE), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(current.toFloat() / total)
                    .height(3.dp)
                    .background(Color.Black, RoundedCornerShape(2.dp))
            )
        }
    }
}


@Composable
fun QuizHeader(current: Int, total: Int, topic: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "←",
            color = Color.White,
            fontSize = 20.sp
        )
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Question $current/$total",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Text(
                text = topic,
                color = Color.Gray,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .border(1.dp, Color.White, RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "$current/$total",
                color = Color.White,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
fun QuizProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(Color(0xFF2A2A2A), RoundedCornerShape(2.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(3.dp)
                .background(Color.White, RoundedCornerShape(2.dp))
        )
    }
}


val pastelColors = listOf(

    Color(0xFFFFF0C7),

    Color(0xFFD2F4E6),

    Color(0xFFD6E7FF),

    Color(0xFFEEDBFF),

    Color(0xFFFFD9D1),

    Color(0xFFFFF5B8),

    Color(0xFFE7C2FF),

    Color(0xFFC7F0FF),

    Color(0xFFFFE4B3),
    Color(0xFFCBE6CE)
)


@Composable
fun QuestionBlock(text: String, modifier: Modifier = Modifier) {

    val bgColor by remember(text) {
        mutableStateOf(
            pastelColors.random()
        )
    }.let { state ->
        animateColorAsState(
            targetValue = state.value,
            animationSpec = tween(500),
            label = "bg"
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            //.background(Color(0xFFF8F8F8))
            .background(bgColor)
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {

        Log.d("THE QUESTION IS",text)

        Column() {





            MathMarkdown(
                text     = text,
                modifier = Modifier.fillMaxWidth()
            )
        }

    }
}


@Composable
fun AnswerOptionButton(
    buttonText: String,
    isSelected: Boolean = false,
    isCorrect: Boolean = false,
    isWrong: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val bgColor by animateColorAsState(
        targetValue = when {
            isCorrect -> Color(0xFFE8F5E9)
            isWrong -> Color(0xFFFFEBEE)
            isSelected -> Color(0xFFF5F5F5)
            else -> Color.White
        },
        animationSpec = tween(200),
        label = "bg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isCorrect -> Color(0xFF4CAF50)
            isWrong -> Color(0xFFEF9A9A)
            isSelected -> Color(0xFF1A1A1A)
            else -> Color(0xFFE5E5E5)
        },
        animationSpec = tween(200),
        label = "border"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isCorrect -> Color(0xFF2E7D32)
            isWrong -> Color(0xFFC62828)
            isSelected -> Color(0xFF1A1A1A)
            else -> Color.Black
        },
        animationSpec = tween(200),
        label = "text"
    )

    Box(modifier = Modifier.fillMaxWidth()) {

        // Visual layer — content, no click handler
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmartMathText(        // ← use SmartMathText here
                    text = buttonText,
                    fontSize = 15.sp,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )
                if (isCorrect || isWrong) {
                    Text(
                        text = if (isCorrect) "✓" else "✗",
                        color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFE53935),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Click layer — sits on top in Z-order, intercepts touches first
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = interactionSource,
                    onClick = onClick
                )
        )
    }
}

@Composable
fun SmartMathText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    color: Color = Color.Black,
    bold: Boolean = false,
    center: Boolean = false
) {
    val processed = text

    if (true) {
        // Complex LaTeX — needs AndroidView
        MathMarkdown(
            text = processed,
            modifier = modifier,
        )
    } else {
        // Plain/Unicode text — native Compose Text, zero click issues
        Text(
            text = processed,
            modifier = modifier,
            fontSize = fontSize,
            color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            textAlign = if (center) TextAlign.Center else TextAlign.Start,
            lineHeight = fontSize * 1.4f
        )
    }
}

@Composable
fun ExplanationBlock(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF8F8F8))
                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(10.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("ℹ", color = Color(0xFF9E9E9E), fontSize = 14.sp)
            MathMarkdown(
                text     = text,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text       = "← scroll left or right to view full equations →",
            fontSize   = 10.sp,
            color      = Color(0xFFBBBBBB),
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth()
        )
    }
}
@Composable
fun ContinueButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = Color.Black,
            contentColor           = Color.White,
            disabledContainerColor = Color(0xFFF0F0F0),
            disabledContentColor   = Color(0xFFBBBBBB)
        )
    ) {
        Text(
            text       = "Continue",
            fontWeight = FontWeight.Bold,
            fontSize   = 16.sp
        )
    }
}


@Composable
fun QuestionCounterPill(
    index: Int,
    total: Int
) {
    val transition = remember { androidx.compose.animation.core.Animatable(1f) }

    LaunchedEffect(index) {
        transition.snapTo(0.85f)
        transition.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.5f,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(200),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .scale(transition.value)
            .alpha(alpha)
            .background(
                color = Color(0xFFF2F2F2),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = "${index + 1} / $total",
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = Color(0xFF6B6B6B)
        )
    }
}
// ─────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────
@Preview
@Composable
fun QuizScreenPreview(topic: String? = "chemical bonding") {
        val context = LocalContext.current
    var currentUser: UserModel? by remember {
        mutableStateOf(
            UserModel(
                userName = "",
                userImage = "",
                userScore = 0,
                userGoogleFirebaseUID = ""
            )
        )
    }

    var myRank by remember { mutableIntStateOf(69) }



    if (topic != null){
        val sampleQuiz = getQuizFile(context =context , hexKey = "40231846d8cabe56b1edbb7058ecf2c8717f341fecc4cb6746b41004e7639bdb", topic = topic)
        if (sampleQuiz != null && currentUser!=null)
            QuizScreen(quizFile = sampleQuiz, onQuizFinished = {_, _, _, _ ->}, currentUser = currentUser, myRank = myRank)

    }
}



@Serializable
data class QuizFile(
    val document: QuizDocument,
    val exportedAt: String,
    val grade: String,
    val subject: String,
    val topic: String,
    val title: String,
    val questionCount: Int,
    val textbooksUsed: List<QuizDocument>,
    val questions: List<QuizQuestion>,
    val systemPrompt: String? = null
)

@Serializable
data class QuizDocument(
    val fileName: String,
    val folderName: String,
    val id: String,
    val pageCount: Int,
    val title: String
)

@Serializable
data class QuizQuestion(
    val id: Int,
    val question: String,
    val answers: List<QuizAnswer>,
    val correct: String,
    val explanation: String
)

@Serializable
data class QuizAnswer(
    val id: String,
    val text: String
)


@Serializable
data class AnsweredQuestion(
    val question: QuizQuestion,
    val selectedId: String?,
    val isCorrect: Boolean,
    val pointsEarned: Int
)


@Serializable
data class UserModel(
    val userName : String,
    val userImage : String,
    val userScore : Int,
    val userGoogleFirebaseUID : String,
)
