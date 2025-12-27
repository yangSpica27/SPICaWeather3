package me.spica.spicaweather3.utils.blur

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

fun Modifier.uniformBlur() = uniformBlurImpl()

@Composable
fun Modifier.progressiveBlur(fromBottom: Boolean = true): Modifier = progressiveBlurImpl(fromBottom)
