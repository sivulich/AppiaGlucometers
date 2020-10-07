# AppiaGlucometers
In this repository you will find the drivers for Android of the glucometers Bioland G-500 and Onetouch Select Plus Flex.

# Project Structure
Each glucometer comes with a service that can read its measurements and configure the glucometer, also a folder with the BLE protocol for each one.

# Usage
To use the drivers you must provide the callbacks defined in BiolandCallbacks and OnetouchCallbacks, the service will call this callbacks when the Measurements are recieved

## Bioland G-500

    public interface BiolandCallbacks extends BleManagerCallbacks {

        /**
         * Called each time the device updates the countdown while performing a measurement.
         * @param aCount
         */
        void onCountdownReceived(int aCount);

        /**
         * Called when new measurements are available. This measurements must be stored by the one who
         * implements this interface.
         * @param aMeasurements
         */
        void onMeasurementsReceived(ArrayList<BiolandMeasurement> aMeasurements);

        /**
         * Called when device information is received.
         * @param aInfo
         */
        void onDeviceInfoReceived(BiolandInfo aInfo);

        /**
         * Called when an error occurs during the communication.
         * @param aMessage
         */
        void onProtocolError(String aMessage);

    }

## Onetouch Select Plus Flex

    public interface OnetouchCallbacks extends BleManagerCallbacks {

        /**
         * Called when new measurements are available. This measurements must be stored by the one who
         * implements this interface.
         * @param aMeasurements
         */
        void onMeasurementsReceived(ArrayList<OnetouchMeasurement> aMeasurements);

        /**
         * Called when device information is received.
         * @param aInfo
         */
        void onDeviceInfoReceived(OnetouchInfo aInfo);

        /**
         * Called when an error occurs during the communication.
         * @param aMessage
         */
        void onProtocolError(String aMessage);
    }




# Techniques Used
## Bioland G-500
The official documentation was used to implement protocols V1, V2, V3.1 and V3.2

## Onetouch Select Plus Flex
Reverse engeenering was used to determine the protocol, the majority of the packets were identified, we attach the investigation files used to determine the meaning of each packet, ONETOUCH-20-001B-EN.pdf.

# Licence
Copyright 2020 Appia Care Inc., Tobías Lifchitz, Santiago Ivulich

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
