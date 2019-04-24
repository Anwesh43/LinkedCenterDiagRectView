package com.anwesh.uiprojects.linkedcenterdiagrectview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.centerdiagrectview.CenterDiagRectView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CenterDiagRectView.create(this)
    }
}
