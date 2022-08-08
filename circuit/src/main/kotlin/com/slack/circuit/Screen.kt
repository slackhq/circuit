package com.slack.circuit

import android.os.Parcelable

/** TODO */
interface Screen : Parcelable

// Example
// Screen implementations are typically data classes
// @Parcelize
// data class AddFavorites(
//  val externalId: UUID,
// ) : Screen

// so when you want to navigate to AddFavorites, you construct an instance and pass it to the
// navigator:
// override fun showAddFavorites() {
//  analytics.logAddMoreFavorites()
//
//  navigator.goTo(
//    AddFavorites(
//      externalId = uuidGenerator.generate()
//    )
//  )
// }
