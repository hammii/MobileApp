package com.example.teamtotest.activity

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.teamtotest.R
import com.example.teamtotest.adapter.FileAdapter
import com.example.teamtotest.dto.FileDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_file.*
import java.text.SimpleDateFormat
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set
import android.app.ProgressDialog
import android.view.View
import android.widget.ProgressBar


class FileActivity : AppCompatActivity() {

    private var firebaseDatabase: FirebaseDatabase? = null
    private var databaseReference: DatabaseReference? = null

    private val fileInfoList: ArrayList<HashMap<String, FileDTO>> = arrayListOf()

    private lateinit var listener: ValueEventListener

    private var PID: String? = null

    lateinit var myAdapter: FileAdapter

    private lateinit var filePath: Uri
    lateinit var progrssBar: ProgressBar
    private var progressDialog: ProgressDialog? = null
    private val START_PROGRESSDIALOG = 100
    private val END_PROGRESSDIALOG = 101


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file)

        setSupportActionBar(file_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val intent = intent /*데이터 수신*/
        PID = intent.extras!!.getString("PID")
    }

    override fun onStart() {
        super.onStart()

        recyclerviewInit()

        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase!!.reference

        setListener_FileInfoFromDB()
    }

    private fun recyclerviewInit() {
        file_recycler_view.setHasFixedSize(true)

        myAdapter = FileAdapter(fileInfoList, this, PID, file_loadingCircle)
        file_recycler_view.adapter = myAdapter
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_file_toolbar, menu)
        return true
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme!! == "content") {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result
    } //파일이름 가져오기

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.upload -> {
                val intent = Intent()
                intent.type = "*/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, "파일을 선택하세요."), 0)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //request코드가 0이고 OK를 선택했고 data에 뭔가가 들어 있다면
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            Log.e("data", data.toString())
            filePath = data!!.data!!
            val fileName: String? = getFileName(filePath)
            uploadFile(fileName)
        }
    }

    private fun uploadFile(filename: String?) { //업로드할 파일이 있으면 수행
        //storage
        val storage = FirebaseStorage.getInstance()

        //storage 주소와 폴더 파일명을 지정해 준다.
        val storageRef = storage.getReferenceFromUrl("gs://teamtogether-bdfc9.appspot.com")

        file_loadingCircle.visibility = View.VISIBLE

        storageRef.child(filename!!).putFile(filePath) //성공시
            .addOnSuccessListener {
                file_loadingCircle.visibility = View.INVISIBLE
                Toast.makeText(applicationContext, "업로드 완료!", Toast.LENGTH_SHORT).show()
                myAdapter.notifyDataSetChanged()
            } //완료
//                .addOnProgressListener {
//                    Toast.makeText(applicationContext, "업로드 중...", Toast.LENGTH_SHORT).show()
//                }
            .addOnFailureListener {
                Toast.makeText(
                    applicationContext,
                    "업로드 실패!",
                    Toast.LENGTH_SHORT
                ).show()
            } //실패
        uploadFileInfoToDB(filename!!)
    }

    private fun uploadFileInfoToDB(fileName: String) {
        val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
        val uid: String = firebaseAuth.currentUser!!.uid
        val userName: String = firebaseAuth.currentUser!!.displayName!!

        val date_format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date = date_format.format(System.currentTimeMillis())

        val fileDTO: FileDTO = FileDTO(fileName, date, uid, userName)

        databaseReference =
            firebaseDatabase!!.reference.child("ProjectList").child(PID.toString()).child("file")
                .push()
        databaseReference!!.setValue(fileDTO)

    }

    fun setListener_FileInfoFromDB() {
        listener = object : ValueEventListener {
            @RequiresApi(Build.VERSION_CODES.KITKAT)
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                fileInfoList.clear()

                for (snapshot in dataSnapshot.children) {
                    val fileId = snapshot.key
                    val fileDTO: FileDTO = snapshot.getValue(FileDTO::class.java)!!

                    val hashMap = HashMap<String, FileDTO>()
                    hashMap[fileId.toString()] = fileDTO

                    fileInfoList.add(hashMap)
                }
                myAdapter.notifyDataSetChanged()
                Log.e("myAdapter", myAdapter.toString())
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(
                    "FileActivity", "loadPost:onCancelled",
                    databaseError.toException()
                )
            }
        }
        databaseReference =
            firebaseDatabase!!.getReference("ProjectList").child(PID.toString()).child("file")
        databaseReference!!.addValueEventListener(listener)

    }


    override fun onStop() {
        databaseReference?.removeEventListener(listener)
        super.onStop()
    }


}