/*
 * Copyright (c)  2018 - 2021 Daniel Quah
 * Copyright (c)  2021 ASDF Dev Pte. Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xyz.hisname.fireflyiii.ui

import android.accounts.AccountManager
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.biometric.BiometricPrompt
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.*
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.*
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.holder.ImageHolder
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.model.*
import com.mikepenz.materialdrawer.model.interfaces.*
import com.mikepenz.materialdrawer.widget.AccountHeaderView
import xyz.hisname.fireflyiii.R
import xyz.hisname.fireflyiii.data.local.account.AuthenticatorManager
import xyz.hisname.fireflyiii.databinding.ActivityBaseBinding
import xyz.hisname.fireflyiii.ui.about.AboutFragment
import xyz.hisname.fireflyiii.ui.account.list.ListAccountFragment
import xyz.hisname.fireflyiii.ui.base.BaseActivity
import xyz.hisname.fireflyiii.ui.bills.list.ListBillFragment
import xyz.hisname.fireflyiii.ui.budget.BudgetListFragment
import xyz.hisname.fireflyiii.ui.categories.CategoriesFragment
import xyz.hisname.fireflyiii.ui.currency.CurrencyListFragment
import xyz.hisname.fireflyiii.ui.dashboard.DashboardFragment
import xyz.hisname.fireflyiii.ui.onboarding.AuthActivity
import xyz.hisname.fireflyiii.ui.transaction.list.TransactionFragment
import xyz.hisname.fireflyiii.ui.piggybank.ListPiggyFragment
import xyz.hisname.fireflyiii.ui.settings.SettingsFragment
import xyz.hisname.fireflyiii.ui.tags.ListTagsFragment
import xyz.hisname.fireflyiii.ui.transaction.addtransaction.AddTransactionActivity
import xyz.hisname.fireflyiii.util.biometric.AuthenticationResult
import xyz.hisname.fireflyiii.util.biometric.Authenticator
import xyz.hisname.fireflyiii.util.biometric.KeyguardUtil
import xyz.hisname.fireflyiii.util.extension.*
import xyz.hisname.fireflyiii.ui.ocr.OcrFragment

class HomeActivity: BaseActivity(){

    private lateinit var headerResult: AccountHeaderView
    private val accountManager by lazy { AuthenticatorManager(AccountManager.get(this))  }
    private val keyguardUtil by lazy { KeyguardUtil(this) }
    private var instanceState: Bundle? = null
    private lateinit var authenticator: Authenticator
    private val homeViewModel by lazy { getViewModel(HomeViewModel::class.java) }
    private lateinit var binding: ActivityBaseBinding
    private val drawerToggle by lazy { ActionBarDrawerToggle(this,
            binding.activityBaseRoot, binding.activityToolbar,
            com.mikepenz.materialdrawer.R.string.material_drawer_open,
            com.mikepenz.materialdrawer.R.string.material_drawer_close) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(accountManager.authMethod.isBlank()|| sharedPref(this).baseUrl.isBlank()){
            AuthenticatorManager(AccountManager.get(this)).destroyAccount()
            val onboardingActivity = Intent(this, AuthActivity::class.java)
            startActivity(onboardingActivity)
            finish()
        } else {
            instanceState = savedInstanceState
            binding = ActivityBaseBinding.inflate(layoutInflater)
            val view = binding.root
            setContentView(view)
            if(keyguardUtil.isAppKeyguardEnabled()){
                authenticator = Authenticator(this, ::handleResult)
                authenticator.authenticate()
            } else {
                setup(savedInstanceState)
                supportActionBar?.setHomeButtonEnabled(true)
            }
        }
    }

    private fun handleResult(result: AuthenticationResult) {
        when (result) {
            is AuthenticationResult.Success -> setup(instanceState)
            is AuthenticationResult.RecoverableError -> displaySnackbar(result.message)
            is AuthenticationResult.UnrecoverableError -> {
                if(result.code == BiometricPrompt.ERROR_NEGATIVE_BUTTON){
                    toastInfo("Authentication cancelled")
                } else {
                    toastError(result.message.toString())
                }
                finish()
            }
            AuthenticationResult.Cancelled -> {
                toastInfo("Cancel")
                finish()
            }
            AuthenticationResult.Failure -> {}
        }
    }

    private fun displaySnackbar(text: CharSequence) {
        Snackbar.make(binding.biggerFragmentContainer, text, Snackbar.LENGTH_INDEFINITE)
                .setAction("Retry") {
                    authenticator.authenticate()
                }
                .show()
    }

    private fun getImageUri() : String? {
        val intent = intent
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND == action && type != null) {
            if (type.startsWith("image/")) {
                return intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM).toString()
            }
        }
        return null
    }

    private fun setup(savedInstanceState: Bundle?){
        animateToolbar()
        setUpHeader(savedInstanceState)
        setSupportActionBar(binding.activityToolbar)
        setUpDrawer(savedInstanceState)
        supportActionBar?.title = ""
        setNavIcon()
        val uri = getImageUri()
        if (intent.getStringExtra("transaction") != null || uri != null) {
            val transaction = intent.getStringExtra("transaction")
            if (uri != null) {
                var intent = Intent(this, AddTransactionActivity::class.java.apply { bundleOf("transactionType" to "Withdrawal")})
                intent.putExtra("uri_image", uri.toString())
                startActivity(intent)
            } else {
                    when (transaction) {
                        "Withdrawal" -> {
                            startActivity(Intent(this, AddTransactionActivity::class.java.apply {
                                bundleOf("transactionType" to "Withdrawal")
                            }))
                        }
                        "Deposit" -> {
                            startActivity(Intent(this, AddTransactionActivity::class.java.apply {
                                bundleOf("transactionType" to "Deposit")
                            }))
                        }
                        "Transfer" -> {
                            startActivity(Intent(this, AddTransactionActivity::class.java.apply {
                                bundleOf("transactionType" to "Transfer")
                            }))
                        }
                    }
            }
        }
        supportFragmentManager.commit {
            replace(R.id.fragment_container, DashboardFragment(), "dash")
        }
    }

    private fun setUpHeader(savedInstanceState: Bundle?){
        val profile = ProfileDrawerItem().apply {
            nameText = AuthenticatorManager(AccountManager.get(this@HomeActivity)).userEmail
            isNameShown = true
            
            descriptionText = sharedPref(this@HomeActivity).userRole
        }
        headerResult = AccountHeaderView(this).apply {
            addProfile(profile,0)
            withSavedInstance(savedInstanceState)
        }
        headerResult.accountHeaderBackground.setBackgroundColor(getCompatColor(R.color.colorAccent))

    }


    private fun setUpDrawer(savedInstanceState: Bundle?){
        val dashboard = PrimaryDrawerItem().apply {
            identifier = 1
            nameRes = R.string.dashboard
            selectedIcon = ImageHolder(IconicsDrawable(this@HomeActivity, GoogleMaterial.Icon.gmd_dashboard).apply {
                sizeDp = 24
                colorRes = R.color.md_deep_orange_500
            })
            icon = ImageHolder(IconicsDrawable(this@HomeActivity, GoogleMaterial.Icon.gmd_dashboard)
                    .apply { sizeDp = 24 })
            isIconTinted = true
        }

        val account = ExpandableDrawerItem().apply {
            nameRes = R.string.account
            identifier = 2
            selectedIcon = ImageHolder(IconicsDrawable(this@HomeActivity,FontAwesome.Icon.faw_credit_card).apply {
                sizeDp = 24
                colorRes = R.color.md_blue_A400
            })
            isIconTinted = true
            icon = ImageHolder(IconicsDrawable(this@HomeActivity, FontAwesome.Icon.faw_credit_card).apply {
                sizeDp = 24
            })
            isSelectable = false
            subItems = mutableListOf(
                    SecondaryDrawerItem().apply {
                        nameRes = R.string.asset_account
                        level = 3
                        identifier = 3
                    },
                    SecondaryDrawerItem().apply {
                        nameRes = R.string.expense_account
                        level = 3
                        identifier = 4
                    },
                    SecondaryDrawerItem().apply {
                        nameRes = R.string.revenue_account
                        level = 3
                        identifier = 5
                    },
                    SecondaryDrawerItem().apply {
                        nameRes = R.string.liability_account
                        level = 3
                        identifier = 21
                    }
            )
        }

        val budgets = PrimaryDrawerItem().apply {
            identifier = 6
            nameRes = R.string.budget
            selectedIcon = ImageHolder(IconicsDrawable(this@HomeActivity, FontAwesome.Icon.faw_chart_pie).apply {
                sizeDp = 24
                colorRes = R.color.md_blue_grey_400
            })
            isIconTinted = true
            icon = ImageHolder(IconicsDrawable(this@HomeActivity, FontAwesome.Icon.faw_chart_pie).apply {
                sizeDp = 24
            })
        }

        val classification = ExpandableDrawerItem().apply {
            nameRes = R.string.classification
            isIconTinted = true
            isSelectable = false
            selectedIcon = ImageHolder(IconicsDrawable(this@HomeActivity,FontAwesome.Icon.faw_tag).apply {
                sizeDp = 24
                colorRes = R.color.md_blue_A400
            })
            icon = ImageHolder(IconicsDrawable(this@HomeActivity, FontAwesome.Icon.faw_tag).apply {
                sizeDp = 24
            })
            subItems = mutableListOf(
                    SecondaryDrawerItem().apply {
                        identifier = 7
                        nameRes = R.string.categories
                        level = 3
                    },
                    SecondaryDrawerItem().apply {
                        identifier = 8
                        level = 3
                        nameRes = R.string.tags
                    }
            )
        }
        val reported = PrimaryDrawerItem().apply {
            identifier = 9
            name = StringHolder("Reports")
        }
        val transactions = ExpandableDrawerItem().apply {
            nameRes = R.string.transaction
            icon = ImageHolder(IconicsDrawable(this@HomeActivity, FontAwesome.Icon.faw_exchange_alt).apply {
                sizeDp = 24
            })
            selectedIcon = ImageHolder(IconicsDrawable(this@HomeActivity, FontAwesome.Icon.faw_exchange_alt).apply {
                sizeDp = 24
                colorRes = R.color.md_amber_600
            })
            isIconTinted = true
            isSelectable = false
            subItems = mutableListOf(
                    SecondaryDrawerItem().apply {
                        nameRes = R.string.withdrawal
                        level = 3
                        identifier = 11
                    },
                    SecondaryDrawerItem().apply {
                        nameRes = R.string.revenue_income_menu
                        level = 3
                        identifier = 12
                    },
                    SecondaryDrawerItem().apply {
                        nameRes = R.string.transfer
                        level = 3
                        identifier = 13
                    }
            )
        }

        val piggyBank = PrimaryDrawerItem().apply {
            nameRes = R.string.piggy_bank
            identifier = 15
            selectedIcon = ImageHolder(IconicsDrawable(this@HomeActivity, FontAwesome.Icon.faw_bullseye).apply {
                sizeDp = 24
                colorRes = R.color.md_red_500
            })
            isIconTinted = true
            icon = ImageHolder(IconicsDrawable(this@HomeActivity, FontAwesome.Icon.faw_bullseye).apply {
                sizeDp = 24
            })
        }
        val bills = PrimaryDrawerItem().apply {
            nameRes = R.string.bill
            selectedIcon = ImageHolder(IconicsDrawable(this@HomeActivity, FontAwesome.Icon.faw_calendar).apply {
                sizeDp = 24
                colorRes = R.color.md_pink_800
            })
            isIconTinted = true
            icon = ImageHolder(IconicsDrawable(this@HomeActivity, FontAwesome.Icon.faw_calendar).apply {
                sizeDp = 24
            })
            identifier = 16
            badgeStyle = BadgeStyle(getColor(R.color.md_red_700), getColor(R.color.md_red_700))
        }
        homeViewModel.getNoOfBillsDueToday().observe(this){ numOfBill ->
            if(numOfBill > 0){
                bills.badge = StringHolder(numOfBill.toString())
                binding.slider.adapter.notifyAdapterDataSetChanged()
            }
        }
        val rules = PrimaryDrawerItem().apply {
            name = StringHolder("Rules")
            level = 4
            identifier = 17
        }

        val options = ExpandableDrawerItem().apply {
            nameRes = R.string.options
            identifier = 14
            icon = ImageHolder(IconicsDrawable(this@HomeActivity, GoogleMaterial.Icon.gmd_settings).apply {
                sizeDp = 24
            })
            isSelectable = false
            isIconTinted = true
            subItems = mutableListOf(
                    SecondaryDrawerItem().apply {
                        nameRes = R.string.settings
                        level = 4
                        identifier = 19
                    },
                    SecondaryDrawerItem().apply {
                        nameRes = R.string.ocr
                        level = 4
                        identifier = 23
                    },
                    SecondaryDrawerItem().apply {
                        nameRes = R.string.currency
                        level = 4
                        isIconTinted = true
                        identifier = 22
                    }
            )
        }

        val about = PrimaryDrawerItem().apply {
            identifier = 20
            nameRes = R.string.about
            selectedIcon = ImageHolder(IconicsDrawable(this@HomeActivity, GoogleMaterial.Icon.gmd_perm_identity).apply {
                sizeDp = 24
                colorRes = R.color.md_pink_500
            })
            isIconTinted = true
            icon = ImageHolder(IconicsDrawable(this@HomeActivity, GoogleMaterial.Icon.gmd_perm_identity).apply {
                sizeDp = 24
            })
        }
        val financialControlSectionHeader = SecondaryDrawerItem().apply { nameRes = R.string.financial_control }
        val accountingSectionHeader = SecondaryDrawerItem().apply { nameRes = R.string.accounting }
        val othersSectionHeader = SecondaryDrawerItem().apply { nameRes = R.string.others }
        binding.slider.apply {
            itemAdapter.add(dashboard, financialControlSectionHeader, budgets, bills, piggyBank,
                    accountingSectionHeader, transactions, othersSectionHeader, account,
                    classification, options, about)
            accountHeader = headerResult
            onDrawerItemClickListener = { v: View?, drawerItem: IDrawerItem<*>, position: Int ->
                when (drawerItem.identifier) {
                    1L -> {
                        supportFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container,
                                        DashboardFragment(), "dash")
                                .commit()
                    }
                    3L -> {
                        val bundle = bundleOf("accountType" to "asset")
                        changeFragment(ListAccountFragment().apply { arguments = bundle })
                    }
                    4L -> {
                        val bundle = bundleOf("accountType" to "expense")
                        changeFragment(ListAccountFragment().apply { arguments = bundle })
                    }
                    5L -> {
                        val bundle = bundleOf("accountType" to "revenue")
                        changeFragment(ListAccountFragment().apply { arguments = bundle })
                    }
                    6L -> {
                        changeFragment(BudgetListFragment())
                    }
                    7L -> {
                        changeFragment(CategoriesFragment())
                    }
                    8L -> {
                        changeFragment(ListTagsFragment())
                    }
                    11L -> {
                        val bundle = bundleOf("transactionType" to "Withdrawal")
                        changeFragment(TransactionFragment().apply { arguments = bundle })
                    }
                    12L -> {
                        val bundle = bundleOf("transactionType" to "Deposit")
                        changeFragment(TransactionFragment().apply { arguments = bundle })
                    }
                    13L -> {
                        val bundle = bundleOf("transactionType" to "Transfer")
                        changeFragment(TransactionFragment().apply { arguments = bundle })
                    }
                    15L -> {
                        changeFragment(ListPiggyFragment())
                    }
                    16L -> {
                        changeFragment(ListBillFragment())
                    }
                    17L -> {
                    }
                    19L -> {
                        changeFragment(SettingsFragment())
                    }
                    20L -> {
                        changeFragment(AboutFragment())
                    }
                    21L -> {
                        val bundle = bundleOf("accountType" to "liabilities")
                        changeFragment(ListAccountFragment().apply { arguments = bundle })
                    }
                    22L -> {
                        changeFragment(CurrencyListFragment())
                    }
                    23L -> {
                        changeFragment(OcrFragment())
                    }
                    else -> {

                    }
                }
                false
            }
            setSavedInstance(savedInstanceState)
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        drawerToggle.isDrawerIndicatorEnabled = true
        binding.activityBaseRoot.addDrawerListener(drawerToggle)
    }

    // sick animation stolen from here: http://frogermcs.github.io/Instagram-with-Material-Design-concept-is-getting-real/
    private fun animateToolbar(){
        val toolbarSize = dpToPx(56)
        binding.activityAppbar.translationY = -toolbarSize.toFloat()
        binding.activityAppbar.animate().translationY(0f).setDuration(300).startDelay = 300
    }

    private fun changeFragment(fragment: Fragment){
        supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment)
        }
    }

    override fun onBackPressed() {
        if(binding.activityBaseRoot.isDrawerOpen(binding.slider)) {
            binding.activityBaseRoot.closeDrawer(binding.slider)
        } else {
            when (supportFragmentManager.backStackEntryCount) {
                0 -> {
                    if (supportFragmentManager.findFragmentByTag("dash") is DashboardFragment) {
                        finish()
                    } else {
                        // This is a dirty hack!
                        supportFragmentManager.fragments.forEach { fragment ->
                            if(fragment is TransactionFragment){
                                supportFragmentManager.beginTransaction()
                                        .replace(R.id.fragment_container,
                                                DashboardFragment(), "dash")
                                        .commit()
                            }
                        }
                        binding.slider.setSelection(1)
                    }
                }
                else -> supportFragmentManager.popBackStack()
            }
        }
    }

    private fun setNavIcon(){
        supportFragmentManager.addOnBackStackChangedListener {
            if(supportFragmentManager.backStackEntryCount >= 1){
                // show back icon and lock nav drawer
                binding.activityBaseRoot.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                drawerToggle.isDrawerIndicatorEnabled = false
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                binding.fabAction.isVisible = false
            } else {
                binding.activityBaseRoot.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
                drawerToggle.isDrawerIndicatorEnabled = true
            }
            supportFragmentManager.fragments.forEach {  fragment ->
                binding.fabAction.isVisible = (fragment is ListBillFragment
                        || fragment is ListPiggyFragment
                        || fragment is TransactionFragment
                        || fragment is ListAccountFragment
                        || fragment is CategoriesFragment
                        || fragment is ListTagsFragment
                        || fragment is CurrencyListFragment
                        || fragment is BudgetListFragment)
            }
        }
        supportFragmentManager.addFragmentOnAttachListener { _, fragment ->
            binding.fabAction.isVisible = (fragment is ListBillFragment
                    || fragment is ListPiggyFragment
                    || fragment is TransactionFragment
                    || fragment is ListAccountFragment
                    || fragment is CategoriesFragment
                    || fragment is ListTagsFragment
                    || fragment is CurrencyListFragment
                    || fragment is BudgetListFragment)
            binding.addTransactionExtended.isVisible = fragment is DashboardFragment
        }
        drawerToggle.setToolbarNavigationClickListener {
            onBackPressed()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(binding.slider?.saveInstanceState(outState) ?: outState)
    }

}