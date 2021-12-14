const express = require("express");
const app = express();
const port = process.env.PORT || 3000;
const firebase = require("firebase");
var admin = require("firebase-admin");
var serviceAccount = require("./messaging-960b8-firebase-adminsdk-pxxck-00703f0ad3.json");
var bodyParser = require("body-parser");
const { auth } = require("firebase-admin");

app.use(express.json());

const firebaseConfig = {
  apiKey: "*",
  authDomain: "messaging-960b8.firebaseapp.com",
  projectId: "messaging-960b8",
  storageBucket: "messaging-960b8.appspot.com",
  messagingSenderId: "576025791890",
  appId: "1:576025791890:web:0040c8d696a4442a67054b",
  measurementId: "G-NML9MG1LN0",
};

const firebaseApp = firebase.initializeApp(firebaseConfig);
const db = firebaseApp.firestore();

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

app.listen(port, () => {
  console.log(`Example app listening at http://localhost:${port}`);
});

app.get("/", (req, res) => {
  res.send("Hello World!");
});

app.post("/friend/:uid/:email", (req, res) => {
  const uid = req.params.uid;
  const email = req.params.email;

  if (email && uid) {
    admin
      .auth()
      .getUserByEmail(email)
      .then((userRecord) => {
        const ref = db.collection("users").doc(uid);
        ref.get().then((doc) => {
          if (!doc.exists) {
            //first friend, create record
            ref.set({ friends: [userRecord["uid"]] });
          } else {
            //not first, append to list
            ref.update({
              friends: firebase.firestore.FieldValue.arrayUnion(
                userRecord["uid"]
              ),
            });
          }

          //add friend for target as well
          const friendRef = db.collection("users").doc(userRecord["uid"]);
          friendRef
            .get()
            .then((friendDoc) => {
              if (!friendDoc.exists) {
                friendRef.set({
                  friends: [uid],
                });
              } else {
                friendRef.update({
                  friends: firebase.firestore.FieldValue.arrayUnion(uid),
                });
              }
            })
            .catch((friendErr) => {});

          admin
            .auth()
            .getUser(uid)
            .then((selfRecord) => {
              db.collection("tokens")
                .doc(userRecord["uid"])
                .get()
                .then((friendToken) => {
                  const token = friendToken.data().token;
                  const selfName = selfRecord["displayName"];
                  var message = {
                    notification: {
                      title: selfName,
                      body: "added you as friend",
                    },
                    data: {
                      type: "friend",
                    },
                    token: token,
                  };
                  admin
                    .messaging()
                    .send(message)
                    .catch((messageError) => {});
                });
            })
            .catch((error) => {});

          res.status(200).send();
        });
      })
      .catch((error) => {
        console.log("Error fetching user data:", error);
        res.status(400).send("Error fetching user data");
      });
  } else res.status(400).send("Missing UID or email parameter");
});

// app.delete("/friend/:uid/:friendUid", (req, res) => {
//   const uid = req.params.uid;
//   const friendUid = req.params.friendUid;

//   if (uid && friendUid) {
//     const ref = db.collection("users").doc(uid);
//     ref
//       .get()
//       .then((doc) => {
//         if (doc.exists) {
//           ref.update({
//             friends: firebase.firestore.FieldValue.arrayRemove(friendUid),
//           });
//         }
//       })
//       .catch((error) => {
//         console.log("error fetching user reference", error);
//       });
//     res.status(200).send();
//   } else res.status(400).send("Missing UID parameters");
// });

app.get("/name/:uid", (req, res) => {
  const uid = req.params.uid;
  if (uid) {
    admin
      .auth()
      .getUser(uid)
      .then((userRecord) => {
        res.send(userRecord["displayName"]);
      })
      .catch((error) => {
        res.status(404).send();
      });
  }
});

app.post("/message/:chatId/:selfUid/:type/:data", (req, res) => {
  const chatId = req.params.chatId;
  const selfUid = req.params.selfUid;
  const type = req.params.type;
  const data = req.params.data;
  if (chatId && selfUid && type && data) {
    //get self name
    admin
      .auth()
      .getUser(selfUid)
      .then((userRecord) => {
        const userName = userRecord["displayName"];
        const ref = db.collection("chats").doc(chatId);
        ref
          .get()
          .then((doc) => {
            if (doc.exists) {
              const oldChat = doc.data();
              const oldMessages = oldChat["messages"];
              const size = oldMessages.length;
              const newMessages = [
                ...oldMessages,
                {
                  id: size,
                  from: selfUid,
                  data: data,
                  type: type,
                },
              ];
              var unread = oldChat["unread"];

              for ([key, value] of Object.entries(unread)) {
                if (key != selfUid) {
                  unread[key] += 1;

                  //send notification
                  db.collection("tokens")
                    .doc(key)
                    .get()
                    .then((tokenDoc) => {
                      if (tokenDoc.exists) {
                        const token = tokenDoc.data().token;
                        var message = {
                          notification: {
                            title: userName,
                            body: "send you a new message",
                          },
                          data: {
                            chatID: chatId,
                            type: "message",
                          },
                          token: token,
                        };
                        admin
                          .messaging()
                          .send(message)
                          .catch((msgError) => {});
                      }
                    })
                    .catch((tokenError) => {});
                }
              }

              ref.update({ messages: newMessages, unread: unread });
              res.send();
            } else res.status(404).send("chat id not found");
          })
          .catch((error) => {});
      })
      .catch((error) => {
        res.status(400).send("failed to fetch user profile");
      });
  } else res.status(400).send("missing parameters");
});
