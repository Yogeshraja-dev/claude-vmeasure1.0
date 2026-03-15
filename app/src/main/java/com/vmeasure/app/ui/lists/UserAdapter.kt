package com.vmeasure.app.ui.lists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vmeasure.app.R
import com.vmeasure.app.data.model.User
import com.vmeasure.app.databinding.ItemUserBinding

class UserAdapter(
    private val onNameClick: (User) -> Unit,
    private val onPinClick: (User, Boolean) -> Unit,
    private val onFavClick: (User, Boolean) -> Unit,
    private val onDeleteClick: (User) -> Unit,
    private val onShareClick: (User) -> Unit,
    private val onLongPress: (User) -> Unit,
    private val onSelectionToggle: (User) -> Unit
) : ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    var isSelectionMode: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var selectedIds: Set<String> = emptySet()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    inner class UserViewHolder(
        private val binding: ItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.tvName.text = user.name

            // Pin icon
            binding.btnPin.setImageResource(
                if (user.isPinned) R.drawable.ic_pin_filled else R.drawable.ic_pin
            )

            // Favourite icon
            binding.btnFavourite.setImageResource(
                if (user.isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite
            )

            // Tag chips
            binding.tagsContainer.removeAllViews()
            user.selectedTags.entries
                .sortedBy { it.key }
                .forEach { (tag, _) ->
                    val chip = LayoutInflater.from(binding.root.context)
                        .inflate(android.R.layout.simple_list_item_1, binding.tagsContainer, false)
                            as TextView
                    chip.apply {
                        text = tag
                        textSize = 12f
                        setPadding(20, 6, 20, 6)
                        setBackgroundResource(R.drawable.bg_tag_chip)
                        val lp = ViewGroup.MarginLayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        lp.marginEnd = 8
                        layoutParams = lp
                    }
                    binding.tagsContainer.addView(chip)
                }

            // Selection mode
            if (isSelectionMode) {
                binding.checkbox.visibility = View.VISIBLE
                binding.checkbox.isChecked = user.userId in selectedIds
                binding.btnPin.visibility = View.GONE
                binding.btnFavourite.visibility = View.GONE
                binding.btnMore.visibility = View.GONE
            } else {
                binding.checkbox.visibility = View.GONE
                binding.btnPin.visibility = View.VISIBLE
                binding.btnFavourite.visibility = View.VISIBLE
                binding.btnMore.visibility = View.VISIBLE
            }

            // Click listeners
            binding.tvName.setOnClickListener {
                if (isSelectionMode) onSelectionToggle(user)
                else onNameClick(user)
            }

            binding.cardContent.setOnClickListener {
                if (isSelectionMode) onSelectionToggle(user)
            }

            binding.root.setOnLongClickListener {
                if (!isSelectionMode) onLongPress(user)
                true
            }

            binding.checkbox.setOnClickListener { onSelectionToggle(user) }

            binding.btnPin.setOnClickListener { onPinClick(user, !user.isPinned) }
            binding.btnFavourite.setOnClickListener { onFavClick(user, !user.isFavorite) }

            binding.btnMore.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menuInflater.inflate(R.menu.user_item_menu, popup.menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menuDelete -> { onDeleteClick(user); true }
                        R.id.menuShare  -> { onShareClick(user);  true }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }

    // ── Adapter overrides ─────────────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ── DiffUtil ──────────────────────────────────────────────────────────────

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(old: User, new: User) = old.userId == new.userId
        override fun areContentsTheSame(old: User, new: User) = old == new
    }
}