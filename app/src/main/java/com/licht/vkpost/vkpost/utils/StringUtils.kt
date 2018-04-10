package com.licht.vkpost.vkpost.utils

import android.graphics.Paint
import kotlin.math.abs
import kotlin.math.max

fun combineLinesByWidth(paint: Paint,
                        lines: List<String>,
                        maxDiffForSmoothing: Int): List<Pair<Int, List<String>>> {
    if (lines.isEmpty())
        return emptyList()

    val res: MutableList<Pair<Int, MutableList<String>>> = mutableListOf()

    var crntWidth = paint.measureText(lines.first())
    var crntGroup: MutableList<String> = mutableListOf()

    for (line in lines) {
        val width = paint.measureText(line)
        if (abs(width - crntWidth) < maxDiffForSmoothing) {
            crntGroup.add(line)
            crntWidth = max(width, crntWidth)
        } else {
            res.add(Pair(crntWidth.toInt(), crntGroup))
            crntWidth = width
            crntGroup = mutableListOf(line)
        }
    }

    res.add(Pair(crntWidth.toInt(), crntGroup))

    return res
}

fun buildStringList(src: String, paint: Paint, maxWidth: Int): List<String> {
    if (src.isEmpty())
        return listOf()

    val words = src.split(' ')

    val res = mutableListOf<String>()

    var currentString = ""


    for (word in words) {
        if (paint.measureText(currentString + word) >= maxWidth) {
            res.add(currentString)

            currentString = word
        } else {
            if (paint.measureText(word) > maxWidth) {
                res.add(word.substring(0, word.length / 2 - 1) + "-")
                res.add(word.substring(word.length / 2))
            } else {
                currentString = currentString.plus(" ").plus(word)
            }


        }
    }
    res.add(currentString)

    return res
}