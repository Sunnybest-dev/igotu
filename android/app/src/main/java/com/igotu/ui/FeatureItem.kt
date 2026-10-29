package com.igotu.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class FeatureItem(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int
)
