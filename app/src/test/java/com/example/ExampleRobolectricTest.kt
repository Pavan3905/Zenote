package com.example

import android.content.Context
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.handleListContinuation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    assertEquals("Zenote", appName)
  }

  @Test
  fun `numbered list continuation works`() {
    val oldVal = TextFieldValue("1. apple")
    val newVal = TextFieldValue("1. apple\n", selection = TextRange(9))
    
    val result = handleListContinuation(oldVal, newVal)
    assertNotNull(result)
    assertEquals("1. apple\n2. ", result!!.text)
    assertEquals(12, result.selection.end)
  }

  @Test
  fun `numbered list double enter escape works`() {
    val oldVal = TextFieldValue("1. apple\n2. ")
    val newVal = TextFieldValue("1. apple\n2. \n", selection = TextRange(16))
    
    val result = handleListContinuation(oldVal, newVal)
    assertNotNull(result)
    assertEquals("1. apple\n\n", result!!.text)
    assertEquals(10, result.selection.end)
  }

  @Test
  fun `bullet list continuation works`() {
    val oldVal = TextFieldValue("- apple")
    val newVal = TextFieldValue("- apple\n", selection = TextRange(8))
    
    val result = handleListContinuation(oldVal, newVal)
    assertNotNull(result)
    assertEquals("- apple\n- ", result!!.text)
    assertEquals(10, result.selection.end)
  }

  @Test
  fun `bullet list double enter escape works`() {
    val oldVal = TextFieldValue("- apple\n- ")
    val newVal = TextFieldValue("- apple\n- \n", selection = TextRange(14))
    
    val result = handleListContinuation(oldVal, newVal)
    assertNotNull(result)
    assertEquals("- apple\n\n", result!!.text)
    assertEquals(9, result.selection.end)
  }
}
