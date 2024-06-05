package com.example.naturecollection

import android.net.Uri
import com.example.naturecollection.PlantRepository.Singleton.databaseRef
import com.example.naturecollection.PlantRepository.Singleton.downloadUri
import com.example.naturecollection.PlantRepository.Singleton.plantList
import com.example.naturecollection.PlantRepository.Singleton.storageReference
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.selects.select
import java.net.URI
import java.util.UUID

class PlantRepository {

    object Singleton {
        // donner le lien pour accéder au bucket
        private const val BUCKET_URL: String = "gs://naturecollection-fc014.appspot.com"

        // se connecter à notre espace de stockage
        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(BUCKET_URL)

        // se connecter à la référence "plants"
        val databaseRef = FirebaseDatabase.getInstance().getReference("plants")

        // créer une liste qui va contenir nos plantes
        val plantList = arrayListOf<PlantModel>()

        // contenir le lien de l'image courant
        var downloadUri: Uri? = null
    }

    fun updateData(callback: () -> Unit) {
        // absorber les données depuis la databaseRef -> liste de plantes
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // retirer les anciennes
                plantList.clear()
                // recolter la liste
                for (ds in snapshot.children) {
                    // construire un object plante
                    val plant = ds.getValue(PlantModel::class.java)

                    // verifier que la plante n'est pas null
                    if(plant != null) {
                        // ajouter la plante à notre liste
                        plantList.add(plant)
                    }
                }
                // actionner le callback
                callback()
            }

            override fun onCancelled(error: DatabaseError) {}

        })
    }

    // créer une fonction pour envoyer des fichiers sur le storage
    fun uploadImage(file: Uri, plantName: String, callback: () -> Unit) {
        // vérifier que ce fichier n'est pas null
        if(file != null) {
            val fileName = plantName + "-" + UUID.randomUUID().toString() + ".jpg"
            val ref = storageReference.child(fileName)
            val uploadTask = ref.putFile(file)

            // demarrer la tache d'envoie
            uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->

                // si il y a eu un problème lors de l'envoi du fichier
                if(!task.isSuccessful) {
                    task.exception?.let { throw it }
                }

                return@Continuation ref.downloadUrl

            }).addOnCompleteListener { task ->
                // verifier si tout a bien fonctionné
                if(task.isSuccessful) {
                    // recuperer l'image
                    downloadUri = task.result
                    callback()
                }
            }
        }
    }

    // mettre à jour l'objet plante en bdd
    fun updatePlant(plant: PlantModel) = databaseRef.child(plant.id).setValue(plant)

    // inserer une nouvelle plante en bdd
    fun insertPlant(plant: PlantModel) = databaseRef.child(plant.id).setValue(plant)

    // supprimer une plante de la base
    fun deletePlant(plant: PlantModel) = databaseRef.child(plant.id).removeValue()

}