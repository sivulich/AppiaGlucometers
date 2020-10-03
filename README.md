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
Reverse engeenering was used to determine the protocol, the majority of the packets were identified, we attach the investigation files used to determine the meaning of each packet.
