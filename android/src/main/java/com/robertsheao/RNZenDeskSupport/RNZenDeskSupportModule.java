/**
 * Created by Patrick O'Connor on 8/30/17.
 * https://github.com/RobertSheaO/react-native-zendesk-support
 */

package com.robertsheao.RNZenDeskSupport;

import android.app.Activity;
import android.content.Intent;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;

import java.util.ArrayList;
import java.util.List;

import zendesk.core.AnonymousIdentity;
import zendesk.core.Zendesk;
import zendesk.support.CustomField;
import zendesk.support.Support;
import zendesk.support.request.RequestActivity;

public class RNZenDeskSupportModule extends ReactContextBaseJavaModule {
  public RNZenDeskSupportModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }
  private static final String TAG = "RN Zendesk";
  private Promise innerPromise;

  @Override
  public String getName() {
    return "RNZenDeskSupport";
  }

  private static long[] toLongArray(ArrayList<?> values) {
    long[] arr = new long[values.size()];
    for (int i = 0; i < values.size(); i++)
      arr[i] = Long.parseLong((String) values.get(i));
    return arr;
  }

  @ReactMethod
  public void initialize(ReadableMap config) {
    String appId = config.getString("appId");
    String zendeskUrl = config.getString("zendeskUrl");
    String clientId = config.getString("clientId");
    Zendesk.INSTANCE.init(getReactApplicationContext(), zendeskUrl, appId, clientId);
    Support.INSTANCE.init(Zendesk.INSTANCE);
  }

  @ReactMethod
  public void setupIdentity(ReadableMap identity) {
    AnonymousIdentity.Builder builder = new AnonymousIdentity.Builder();

    if (identity != null && identity.hasKey("customerEmail")) {
      builder.withEmailIdentifier(identity.getString("customerEmail"));
    }

    if (identity != null && identity.hasKey("customerName")) {
      builder.withNameIdentifier(identity.getString("customerName"));
    }

    Zendesk.INSTANCE.setIdentity(builder.build());
  }

  @ReactMethod
  public void callSupport(ReadableMap customFields) {

    List<CustomField> fields = new ArrayList<>();

    ReadableMapKeySetIterator iterator = customFields.keySetIterator();
    while (iterator.hasNextKey()) {
      String key = iterator.nextKey();
      String value = customFields.getString(key);
      fields.add(new CustomField(Long.parseLong(key), value));
    }

    Activity activity = getCurrentActivity();

    if(activity != null){
      Intent requestActivityIntent = RequestActivity.builder()
              .withCustomFields(fields)
              .intent(activity);
      requestActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      getReactApplicationContext().startActivity(requestActivityIntent);
    }
  }
}
