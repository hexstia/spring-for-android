package com.hexstia.spring_for_android;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import org.springframework.aop.Advice;


public class MainActivity extends AppCompatActivity {
//移植spring到android平台
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Advice advisor = new Advice();
        advisor.test();
    }

}