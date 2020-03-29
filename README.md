
APP'S FUNCTIONALITY:

The app's UI contains two buttons,  identify button (for capturing and identifying the image) and send button (for sending the image to the database).

WORKFLOW:

The app first captures the image and then extracts text from it. Based on the text extracted from the image and the results of the TensorFlow model(which is trained to identify the image of pan card and aadhar card), the app classifies the image as Aadhar Card or Pan Card.
If the image is neither a pan card nor an aadhar card, a message is sent to the user that the card is invalid.
Further, if the image is identified to be an aadhar card and the user clicks on the send button which is present on the UI, the information on the aadhar card scanned, is sent to  the database which is meant for storing ONLY aadhar card details.
If the image is identified to be a pan card and the user clicks on the send button which is present on the UI, the information on the pan card scanned, is sent to the database which is meant for storing ONLY pan card details.

PROCESS:

The text is extracted with the help of OCR(Optical Character Recognition). On capturing the image and clicking on the identify button(which is present on UI), run text recognition method is called which converts the captured image in an appropriate form so that the captured image can be fed to the OCR engine. Then the text detector(not text extractor) of the OCR engine is initialized and the image is fed to it. If the process succeeds process text method is called which extracts the text from the image through a machine learning algorithm. The algorithm first initializes a block of the image, then it initializes a line in that block and then finally it initializes a particular element in that block and extracts the text.
This is continued in a loop till we reach the end. The text is overlayed on screen by the Graphic Overlay Java class.
After that, we need to feed our image in the TensorFlow model. To do this, the image is rescaled to an input size that the model can accept. Then the image which is in bitmap form is converted bytebuffer format because the model only accepts a bytebuffer as input. To convert it into bytebuffer the image's pixels in each of the RGB channels are processed and normalized for all the three channels separately and are simultaneously put into a 1-D byte array. This byte array is fed to the TensorFlow model, and the output is stored in results. The results is a 2-D array of size (1,2) and it contains the probability of the image being a pan card or aadhar card. Along with this, we are also analyzing the text extracted to classify the image. Both of the above two outputs work together to classify the image. 
Further, I have made two separate databases for storing details of AADHAR card and PAN card. If the image is classified as aadhar card, its details are stored in aadhar card database and if the image is classified as pan card, its details are stored in the pan card database. For storing the details we have called the AsyncTask method which converts the data to a JSON response which is further sent to the database where it is decoded and the details are stored.

