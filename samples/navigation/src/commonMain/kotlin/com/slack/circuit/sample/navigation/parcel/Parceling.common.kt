package com.slack.circuit.sample.navigation.parcel

// For Android @Parcelize
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class CommonParcelize

// For Android Parcelable
expect interface CommonParcelable
