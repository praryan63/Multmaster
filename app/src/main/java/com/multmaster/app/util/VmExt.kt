package com.multmaster.app.util

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.multmaster.app.MultiplicationApp

fun CreationExtras.app(): MultiplicationApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MultiplicationApp