package com.krystal.bull.core.gui.dialog

import com.krystal.bull.core.gui.GlobalData
import com.krystal.bull.core.gui.GlobalData._
import org.bitcoins.commons.jsonmodels.dlc.DLCMessage.OracleInfo
import org.bitcoins.dlc.oracle._
import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control._
import scalafx.scene.layout.{GridPane, HBox}
import scalafx.stage.Window

object ViewEventDialog {

  def showAndWait(parentWindow: Window, event: Event): Unit = {
    val dialog = new Dialog[Unit]() {
      initOwner(parentWindow)
      title = event.eventName
    }

    dialog.dialogPane().buttonTypes = Seq(ButtonType.Close)
    dialog.dialogPane().stylesheets = GlobalData.currentStyleSheets

    dialog.dialogPane().content = new GridPane() {
      hgap = 10
      vgap = 10
      padding = Insets(20, 100, 10, 10)

      add(new Label("Oracle Info:"), 0, 0)
      add(new TextField() {
            text = OracleInfo(oracle.publicKey, event.nonce).hex
            editable = false
          },
          columnIndex = 1,
          rowIndex = 0)

      add(new Label("Nonce:"), 0, 1)
      add(new TextField() {
            text = event.nonce.hex
            editable = false
          },
          columnIndex = 1,
          rowIndex = 1)

      add(new Label("Num Outcomes:"), 0, 2)
      add(new TextField() {
            text = event.numOutcomes.toString
            editable = false
          },
          columnIndex = 1,
          rowIndex = 2)

      add(new Label("Signing Version:"), 0, 3)
      add(new TextField() {
            text = event.signingVersion.toString
            editable = false
          },
          columnIndex = 1,
          rowIndex = 3)

      add(new Label("Commitment Signature:"), 0, 4)
      add(new TextField() {
            text = event.commitmentSignature.hex
            editable = false
          },
          columnIndex = 1,
          rowIndex = 4)

      add(new Label("Attestation:"), 0, 5)
      event match {
        case completed: CompletedEvent =>
          add(new TextField() {
                text = completed.signature.hex
                editable = false
              },
              columnIndex = 1,
              rowIndex = 5)
        case _: PendingEvent =>
          var outcomeOpt: Option[String] = None

          val outcomeSelector: ComboBox[String] = new ComboBox(event.outcomes) {
            onAction = (_: ActionEvent) => {
              outcomeOpt = Some(value.value)
            }
          }

          val button = new Button("Sign") {
            onAction = _ =>
              outcomeOpt match {
                case Some(outcome) =>
                  new Alert(AlertType.Confirmation) {
                    initOwner(owner)
                    title = "Confirm Signing"
                    contentText =
                      s"Are you sure you would like sign to the outcome $outcome?"
                    dialogPane().stylesheets = GlobalData.currentStyleSheets
                  }.showAndWait() match {
                    case Some(ButtonType.OK) =>
                      GlobalData.oracle
                        .signEvent(event.nonce, outcome)
                    case None | Some(_) =>
                      ()
                  }
                case None =>
                  new Alert(AlertType.Error) {
                    initOwner(owner)
                    title = "Error!"
                    contentText = "Need to select an outcome to sign"
                    dialogPane().stylesheets = GlobalData.currentStyleSheets
                  }.showAndWait()
              }
          }

          val hBox = new HBox(outcomeSelector, button) {
            spacing = 10
          }

          add(hBox, columnIndex = 1, rowIndex = 5)
      }
    }

    val _ = dialog.showAndWait()
  }
}
