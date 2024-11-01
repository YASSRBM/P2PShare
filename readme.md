Ce projet permet de développer une application  pour le partage de fichier en mode pair à pair 
#Connexion 
Pour utiliser le systéme, nous pouvons ouvrir un terminal et exécuter "make tracker" ensuite ouvrir un deuxiéme terminal et éxécuter "make test-peer"

#Connexion avec des paramếtres 

Pour lancer 2 peer 

java project.Peer config.ini peer2filespath/"fichier1" peer2filespath/"fichier2"
java project.Peer config.ini peer1filespath/"fichier1" peer1filespath/"fichier2"

"announce" permet d'annoncer les données du peer au tracker 


