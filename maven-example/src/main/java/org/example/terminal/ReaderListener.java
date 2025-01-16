package org.example.terminal;

import com.stripe.stripeterminal.external.callable.Cancelable;
import com.stripe.stripeterminal.external.callable.InternetReaderListener;
import com.stripe.stripeterminal.external.callable.MobileReaderListener;
import com.stripe.stripeterminal.external.models.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReaderListener implements InternetReaderListener, MobileReaderListener {
    Cancelable cancelable;
/**
*
 * @param batteryLevel
 * @param batteryStatus
 * @param isCharging
*/
    @Override
    public void onBatteryLevelUpdate(float batteryLevel, @NotNull BatteryStatus batteryStatus, boolean isCharging) {
        MobileReaderListener.super.onBatteryLevelUpdate(batteryLevel, batteryStatus, isCharging);
        System.out.printf("Battery Update %.2f, %s, %s \n", batteryLevel, batteryStatus, isCharging);
    }

/**
*
 * @param update
 * @param e
*/
    @Override
    public void onFinishInstallingUpdate(@Nullable ReaderSoftwareUpdate update, @Nullable TerminalException e) {
        MobileReaderListener.super.onFinishInstallingUpdate(update, e);
        cancelable = null;
        if (e == null) {
            System.out.printf("Finished installing update %s \n", update);
        } else {
            System.out.printf("Failed to install update %s \n", update);
            e.printStackTrace();
        }
    }

/**
*
 * @param update
*/
    @Override
    public void onReportAvailableUpdate(@NotNull ReaderSoftwareUpdate update) {
        MobileReaderListener.super.onReportAvailableUpdate(update);
        System.out.printf("Available update %s \n", update);
    }

/**
*
*/
    @Override
    public void onReportLowBatteryWarning() {
        MobileReaderListener.super.onReportLowBatteryWarning();
        System.out.println("Low Battery Warning");
    }

/**
*
 * @param progress
*/
    @Override
    public void onReportReaderSoftwareUpdateProgress(float progress) {
        MobileReaderListener.super.onReportReaderSoftwareUpdateProgress(progress);
        System.out.printf("Update Progress %.2f \n", progress);
    }

/**
*
 * @param message
*/
    @Override
    public void onRequestReaderDisplayMessage(@NotNull ReaderDisplayMessage message) {
        MobileReaderListener.super.onRequestReaderDisplayMessage(message);
        System.out.printf("Display Message %s \n", message);
    }

/**
*
 * @param options
*/
    @Override
    public void onRequestReaderInput(@NotNull ReaderInputOptions options) {
        MobileReaderListener.super.onRequestReaderInput(options);
        System.out.printf("Request Reader Input %s \n", options);
    }

/**
*
 * @param update
 * @param cancelable
*/
    @Override
    public void onStartInstallingUpdate(@NotNull ReaderSoftwareUpdate update, @Nullable Cancelable cancelable) {
        MobileReaderListener.super.onStartInstallingUpdate(update, cancelable);
        this.cancelable = cancelable;
        System.out.printf("Start installing update %s \n", update);
    }

/**
*
 * @param reason
*/
    @Override
    public void onDisconnect(@NotNull DisconnectReason reason) {
        System.out.printf("Disconnected %s \n", reason);
        throw new RuntimeException("Disconnected due to " + reason);
    }

/**
*
 * @param event
*/
    @Override
    public void onReportReaderEvent(@NotNull ReaderEvent event) {
        MobileReaderListener.super.onReportReaderEvent(event);
        System.out.printf("Report Reader Event %s \n", event);
    }

/**
*
 * @param reader
*/
    @Override
    public void onReaderReconnectFailed(@NotNull Reader reader) {
        MobileReaderListener.super.onReaderReconnectFailed(reader);
        System.out.printf("Reconnect Failed %s \n", reader);
        cancelable = null;
    }

/**
*
 * @param reader
 * @param cancelReconnect
 * @param reason
*/
    @Override
    public void onReaderReconnectStarted(@NotNull Reader reader, @NotNull Cancelable cancelReconnect, @NotNull DisconnectReason reason) {
        MobileReaderListener.super.onReaderReconnectStarted(reader, cancelReconnect, reason);
        this.cancelable = cancelReconnect;
        System.out.printf("Reconnect Started %s, %s, %s \n", reader, cancelReconnect, reason);
    }

/**
*
 * @param reader
*/
    @Override
    public void onReaderReconnectSucceeded(@NotNull Reader reader) {
        MobileReaderListener.super.onReaderReconnectSucceeded(reader);
        System.out.printf("Reconnect Succeeded %s \n", reader);
        this.cancelable = null;
    }
}
