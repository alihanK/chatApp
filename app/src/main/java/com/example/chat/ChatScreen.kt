package com.example.chat

import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Environment
import android.os.Environment.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage

import com.example.chatktln.R
import com.example.chatktln.ui.theme.Purple80
import com.example.home.ChannelItem
import com.example.model.Message
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.contracts.contract


@Composable
fun ChatScreen(navController: NavController, channelId: String, channelName: String){
    Scaffold (
        containerColor = Color.White
    ){
        val viewModel: ChatViewModel = hiltViewModel()
        val chooseDialog = remember {
            mutableStateOf(false)
        }

        val cameraImageUrl = remember {
            mutableStateOf<Uri?>(null)
        }

        val cameraImageLauncher = rememberLauncherForActivityResult (
            contract = ActivityResultContracts.TakePicture()
        ){
            success ->
            if (success){
                cameraImageUrl.value?.let{
                   viewModel.resimliMesajigonderme(it, channelId)
                }

            }
        }

        val imageLauncher = rememberLauncherForActivityResult (
            contract = ActivityResultContracts.GetContent()
        ){
                uri:Uri?  ->
                uri?.let { viewModel.resimliMesajigonderme(it, channelId) }
        }

        fun createImageUrl():Uri{
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = ContextCompat.getExternalFilesDirs(
                navController.context,
                Environment.DIRECTORY_PICTURES
            ).first()
            return  FileProvider.getUriForFile(
                navController.context,
                "${navController.context.packageName}.provider",
                File.createTempFile("JPEG_${timestamp}_", ".jpg", storageDir).apply {
                    cameraImageUrl.value=Uri.fromFile(this)
                }
            )
        }
        
        val permissionLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {isGranted->
                if(isGranted){
                     cameraImageLauncher.launch(createImageUrl())
                }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)) {
            val viewModel: ChatViewModel = hiltViewModel()
            LaunchedEffect(key1 = true) {
                viewModel. mesajBirimi(channelId)
            }
            val messages = viewModel.message.collectAsState()
            ChatMessages(
                messages = messages.value,
                onSendMessage = { message ->
                    viewModel.mesajGonderme(channelId, message)
                },
                onImageClicked = {chooseDialog.value=true},
                channelName = channelName
            )
        }



            if(chooseDialog.value){
                onSelectedDialog(onCameraSelected = {
                    chooseDialog.value=false
                    if(navController.context.checkSelfPermission(android.Manifest.permission.CAMERA)==android.content.pm.PackageManager.PERMISSION_GRANTED){
                        cameraImageLauncher.launch(createImageUrl())
                    }
                    else{
                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                }, onGallerySelected = {
                    chooseDialog.value=false
                    imageLauncher.launch("image/*")
                })
            }
    }
}

@Composable
fun onSelectedDialog(onCameraSelected: () -> Unit, onGallerySelected: () -> Unit){
    AlertDialog(
        onDismissRequest = {

        },
        confirmButton = { 
            TextButton(onClick = onCameraSelected
            )  {
                Text(text = "CAMERA")
            }
                        },
        dismissButton = {
            TextButton(onClick = onGallerySelected
            )  {
                Text(text = "GALLERY")
            }
        },
        title = { Text(text = "SELECT A RESOURCE") },
        text = { Text(text = "Take a picture or choose one from your gallery") }
        )
}

@Composable
fun ChatMessages(
    channelName: String,
    messages: List<Message>,
    onSendMessage: (String) -> Unit,
    onImageClicked: ()-> Unit,
)
{
    val klavyeKontrolGizleme = LocalSoftwareKeyboardController.current
    val msg = remember {
        mutableStateOf("")
    }
    Column(modifier = Modifier.fillMaxSize()){
        LazyColumn (modifier=Modifier.weight(1f)){
            item {
                ChannelItem(channelName=channelName){}
            }
            items(messages) {message->
                ChatBirimleri(message = message)
            }
        }
        Row (
            modifier = Modifier
                .imePadding()
                .fillMaxWidth()
                .background(Color.LightGray)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ){
            IconButton(onClick = {
                msg.value=""
                onImageClicked()
            }) {
                Image(painter = painterResource(id = R.drawable.attach), contentDescription = "send")
            }

            TextField(
                value = msg.value, onValueChange = {msg.value = it},
                modifier = Modifier.weight(1f),
                placeholder = {Text(text ="Text message")},
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                       klavyeKontrolGizleme?.hide()
                    }
                )
            )
            IconButton(onClick = {
                onSendMessage(msg.value)
                msg.value=""
            }) {
                Image(painter = painterResource(id = R.drawable.send), contentDescription = "send")
            }

        }
    }
}

@Composable
fun ChatBirimleri(message: Message){
    val aktifKullanici = message.gonderenId == Firebase.auth.currentUser?.uid
    val birimRengi = if (aktifKullanici){
        Purple80
    }
    else {
        Color.LightGray
    }

    Box (modifier = Modifier
        .fillMaxWidth()
        .padding(
            vertical = 4.dp, horizontal = 8.dp
        )
        )
    {
        val alignment = if (!aktifKullanici) Alignment.CenterStart else Alignment.CenterEnd
        Row(
                modifier = Modifier
                    .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .align(alignment),
            verticalAlignment = Alignment.CenterVertically

            )
            {
                if (!aktifKullanici){
                    Image(painter = painterResource(id = R.drawable.friend), contentDescription = null, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(8.dp) )
                }
                Box(
                    modifier = Modifier.
                    background(color = birimRengi, shape = RoundedCornerShape(8.dp))
                        .padding(16.dp)){
                if(message.imageUrl!=null){
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(200.dp),
                        contentScale = ContentScale.Crop
                        )
                }
                else {
                    Text(
                        text = message.mesaj?.trim()?:"",
                        color = Color.White)
                }

                }

            }
    }
}