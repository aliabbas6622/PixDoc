package com.pixdoc

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PixDoc", appName)
  }

  @Test
  fun `browser view model can be created by AndroidViewModelFactory`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    val viewModel = factory.create(com.pixdoc.feature.browser.BrowserViewModel::class.java)
    org.junit.Assert.assertNotNull(viewModel)
  }

  @Test
  fun `office view model can be created by AndroidViewModelFactory`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    val viewModel = factory.create(com.pixdoc.feature.office.OfficeViewModel::class.java)
    org.junit.Assert.assertNotNull(viewModel)
  }

  @Test
  fun `document converter creates blank note and blank spreadsheet`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val noteRes = com.pixdoc.core.utils.DocumentConverter.createBlankNote(context, "Test_Note", "Hello Office")
    org.junit.Assert.assertTrue(noteRes.isSuccess)
    val noteFile = noteRes.getOrNull()
    org.junit.Assert.assertNotNull(noteFile)
    org.junit.Assert.assertTrue(noteFile!!.exists())

    val sheetRes = com.pixdoc.core.utils.DocumentConverter.createBlankSpreadsheet(context, "Test_Sheet")
    org.junit.Assert.assertTrue(sheetRes.isSuccess)
  }
}
