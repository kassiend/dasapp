package kz.das.dasaccounting.ui.parent_bottom.profile

import kz.das.dasaccounting.R
import kz.das.dasaccounting.core.navigation.DasAppScreen
import kz.das.dasaccounting.core.navigation.requireRouter
import kz.das.dasaccounting.core.ui.dialogs.ActionConfirmDialog
import kz.das.dasaccounting.core.ui.fragments.BaseFragment
import kz.das.dasaccounting.databinding.FragmentProfileBinding
import kz.das.dasaccounting.ui.parent_bottom.hideBottomNavMenu
import kz.das.dasaccounting.ui.parent_bottom.profile.history.ProfileHistoryFragment
import kz.das.dasaccounting.ui.parent_bottom.profile.support.ProfileSupportFragment
import org.koin.android.viewmodel.ext.android.viewModel

class ProfileFragment: BaseFragment<ProfileVM, FragmentProfileBinding>() {

    companion object {
        fun getScreen() = DasAppScreen(ProfileFragment())
    }

    override val mViewModel: ProfileVM by viewModel()

    override fun getViewBinding() = FragmentProfileBinding.inflate(layoutInflater)

    override fun setupUI() {
        mViewBinding.run {
            lifecycleOwner = viewLifecycleOwner
            profileVM = mViewModel
            clProfile.setOnClickListener {
                hideBottomNavMenu()
                requireRouter().navigateTo(ProfileInfoFragment.getScreen())
            }
            this.rlHistory.setOnClickListener {
                hideBottomNavMenu()
                requireRouter().navigateTo(ProfileHistoryFragment.getScreen())
            }
            this.rlSupport.setOnClickListener {
                hideBottomNavMenu()
                requireRouter().navigateTo(ProfileSupportFragment.getScreen())
            }
            this.rlLogOut.setOnClickListener {
                showLogOutDialog()
            }
        }
    }

    private fun showLogOutDialog() {
        val actionDialog = ActionConfirmDialog.Builder()
                .setCancelable(true)
                .setTitle(getString(R.string.log_out_title))
                .setDescription(getString(R.string.log_out_desc))
                .setOnConfirmCallback(object : ActionConfirmDialog.OnConfirmCallback {
                    override fun onConfirmClicked() {
                        this@ProfileFragment.onLogout()
                    }
                    override fun onCancelClicked() { }
                }).build()
        actionDialog.show(childFragmentManager, ActionConfirmDialog.TAG)
    }


}