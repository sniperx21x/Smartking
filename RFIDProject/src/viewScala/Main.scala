package viewScala

import barrier.controller._
import viewJava._
import com.phidgets._
import com.phidgets.event._
import javax.swing._ 
import scala.util._

object Main {
  def main(args: Array[String]) {
    
    val barriere = new Barriere()
    barriere.Barriere()
    val rfidController = RFID
    val rfid = new RFIDPhidget()
    rfidController.RFID(rfid)
    
    val panelWelcome = (new PanelWelcome {
      
      override def actionScalaForm() 
      {
         if (combo.getSelectedItem() == "Inscription") {
            rfidController.action = "write"
            south.removeAll()
            south.revalidate()
            south.repaint()
            south.add(inscription)
            south.add(update)
            userNom.setEditable(true) 
            userPrenom.setEditable(true)
            userMail.setEditable(true)
         } else {
            south.removeAll()
            south.revalidate()
            south.repaint()
            south.add(in)
            south.add(out)
            south.add(open)
            south.add(close)
            userNom.setEditable(false)
            userPrenom.setEditable(false)
            userMail.setEditable(false)
         }
      }
      
      override def actionScalaIn() 
      {
         rfidController.in()  
      }

      override def actionScalaOut() 
      {
         rfidController.out()  
      }

      override def actionScalaOpen() 
      {
        barriere.ouverture()
      }

      override def actionScalaClose() 
      {
        barriere.fermeture()
      }
      
      override def actionScalaWrite() 
      {
          val tag = rfidController.genTag()
          rfidController.register(tag, userNom.getText, userPrenom.getText, userMail.getText) match {
              case Success(rep) => {
                  rfid.write(tag, RFIDPhidget.PHIDGET_RFID_PROTOCOL_PHIDGETS, false) //écrit sur la tag
                  println("\nWrite Tag : " + tag)
                  rfid.setOutputState(1, true) //ecriture : vert si tag a bien été écrit
                  //Boîte du message d'information
                  JOptionPane.showMessageDialog(null, "L'utilisateur est bien inscrit", "Inscription", JOptionPane.INFORMATION_MESSAGE);
                  rfid.setOutputState(1, false)
              }
              case Failure(exc) => {
                  println(exc)
                  rfid.setOutputState(0, true)
                  JOptionPane.showMessageDialog(null, "L'utilisateur n'a pas été inscrit", "Inscription", JOptionPane.ERROR_MESSAGE);
                  rfid.setOutputState(0, false)
              }
          }
      }
      
      override def actionScalaUpdate() 
      {
          
      }
      
      rfid.addTagGainListener(new TagGainListener() {
        def tagGained(oe: TagGainEvent) {
          val tag = oe.getValue
          println("\nTag Gained: " + tag)
          tagLu.setText(tag) 
          if(rfidController.action != "write") {
            
              val user = rfidController found tag  
              user match {
                case Some(userJson) => {
                  userNom.setText(userJson.get("lastname").toString)
                  userPrenom.setText(userJson.get("firstname").toString)
                  userMail.setText(userJson.get("mail").toString)
                  client.setSelected(true)
                  
                  val idUser = userJson.get("id").toString
                  rfidController passed tag match {
                    case true => {
                        rfid.setOutputState(1, true) //vert si bien passé
                        barriere.ouverture
                        //Boîte du message d'information
                        JOptionPane.showMessageDialog(null, "L'utilisateur est bien passé", "Passage", JOptionPane.INFORMATION_MESSAGE);             
                        
                        rfidController.carPassed(tag) //TODO
                        
                        barriere.fermeture
                    }
                    case false => {
                        rfid.setOutputState(0, true) //rouge si le tag est présent en BD
                        JOptionPane.showMessageDialog(null, "L'utilisateur n'a pas pu passer", "Passage", JOptionPane.ERROR_MESSAGE);           
                    }
                    case _ =>
                  }
                }
                case None => {
                  userNom.setText("")
                  userPrenom.setText("")
                  userMail.setText("")
                  client.setSelected(false)
                  rfid.setOutputState(0, true) //rouge si le tag est présent en BD
                  JOptionPane.showMessageDialog(null, "L'utilisateur n'existe pas", "Passage", JOptionPane.ERROR_MESSAGE);                       
                }
              }
          }
        }
      })
      
      
    })
    
    rfid.addDetachListener(new DetachListener() {
        def detached(ae: DetachEvent) {
          println("detachment  1 of " + ae)
          JOptionPane.showMessageDialog(null, "Veuillez rattacher le Phidget RFID", "RFID detached", JOptionPane.WARNING_MESSAGE);                  
        }
      })
      
    rfid.addErrorListener(new ErrorListener() {
        def error(ee: ErrorEvent) {
          println("error event for " + ee)
          JOptionPane.showMessageDialog(null, "Il y a eu une erreur pour l'événement " + ee, "RFID error", JOptionPane.ERROR_MESSAGE);           
        }
      }) 
    
    val MainJFrame = new MainJFrame(panelWelcome)
  }  
}