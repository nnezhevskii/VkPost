package com.licht.vkpost.vkpost.view


import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.licht.vkpost.vkpost.PostActivity

import com.licht.vkpost.vkpost.R
import com.licht.vkpost.vkpost.StickerAdapter
import com.licht.vkpost.vkpost.data.model.Sticker


/**
 * A simple [Fragment] subclass.
 */
class BottomSheetFragment : BottomSheetDialogFragment(), IStickerSelector {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(context, R.style.BottomSheetDialog)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater!!.inflate(R.layout.fmt_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = StickerAdapter(this)

        val rvStickers = view.findViewById<RecyclerView>(R.id.rv_stickers)
        rvStickers.layoutManager = GridLayoutManager(context, 4)
        rvStickers.adapter = adapter

        adapter.setData(Sticker.getStickers(context))

    }

    override fun onStickerSelecter(sticker: Sticker) {
        (activity as PostActivity).addSticker(sticker)
        dismiss()
    }
}// Required empty public constructor
