/**
 * Created by Patrick O'Connor on 8/30/17.
 * https://github.com/RobertSheaO/react-native-zendesk-support
 */

package com.robertsheao.RNZenDeskSupport;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableMap;
import com.zendesk.service.ErrorResponse;
import com.zendesk.service.ZendeskCallback;

import java.util.ArrayList;
import java.util.List;

import zendesk.core.AnonymousIdentity;
import zendesk.core.Zendesk;
import zendesk.support.CreateRequest;
import zendesk.support.CustomField;
import zendesk.support.Request;
import zendesk.support.RequestProvider;
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

//  @ReactMethod
//  public void showHelpCenterWithOptions(ReadableMap options) {
//    SupportActivityBuilder.create()
//            .withOptions(options)
//            .show(getReactApplicationContext());
//  }
//
//  @ReactMethod
//  public void showCategoriesWithOptions(ReadableArray categoryIds, ReadableMap options) {
//    SupportActivityBuilder.create()
//            .withOptions(options)
//            .withArticlesForCategoryIds(categoryIds)
//            .show(getReactApplicationContext());
//  }
//
//  @ReactMethod
//  public void showSectionsWithOptions(ReadableArray sectionIds, ReadableMap options) {
//    SupportActivityBuilder.create()
//            .withOptions(options)
//            .withArticlesForSectionIds(sectionIds)
//            .show(getReactApplicationContext());
//  }
//
//  @ReactMethod
//  public void showLabelsWithOptions(ReadableArray labels, ReadableMap options) {
//    SupportActivityBuilder.create()
//            .withOptions(options)
//            .withLabelNames(labels)
//            .show(getReactApplicationContext());
//  }
//
//  @ReactMethod
//  public void showHelpCenter() {
//    showHelpCenterWithOptions(null);
//  }
//
//  @ReactMethod
//  public void showCategories(ReadableArray categoryIds) {
//    showCategoriesWithOptions(categoryIds, null);
//  }
//
//  @ReactMethod
//  public void showSections(ReadableArray sectionIds) {
//    showSectionsWithOptions(sectionIds, null);
//  }
//
//  @ReactMethod
//  public void showLabels(ReadableArray labels) {
//    showLabelsWithOptions(labels, null);
//  }

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
              .withRequestSubject("Android ticket")
              .withCustomFields(fields)
              .intent(activity);
      requestActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      getReactApplicationContext().startActivity(requestActivityIntent);
    }
  }

  @ReactMethod
  public void supportHistory() {

    Activity activity = getCurrentActivity();

    if(activity != null){
      Intent supportHistoryIntent = new Intent(getReactApplicationContext(), RequestActivity.class);
      supportHistoryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      getReactApplicationContext().startActivity(supportHistoryIntent);
    }
  }

  @ReactMethod
  public void createRequest(
          ReadableMap request,
          Promise promise) {
    try {
      // Get an instance of the RequestProvider from the ZendeskConfig
      RequestProvider provider = Support.INSTANCE.provider().requestProvider();

      // Build the request object from the javascript arguments
      CreateRequest zdRequest = new CreateRequest();

      zdRequest.setSubject(request.getString("subject"));
      zdRequest.setDescription(request.getString("requestDescription"));

      ReadableArray list = request.getArray("tags");
      List<String> tagsList = new ArrayList<>(list.size());
      for (int i = 0; i < list.size(); i++) {
        tagsList.add(list.getString(i));
      }

      zdRequest.setTags(tagsList);

      innerPromise = promise;
      // Create thew ZendeskCallback.
      ZendeskCallback<Request> callback = new ZendeskCallback<Request>() {
        @Override
        public void onSuccess(Request createRequest) {
          Log.d(TAG, "onSuccess: Ticket created!");

          WritableMap map = Arguments.createMap();
          WritableMap request = Arguments.createMap();

          map.putString("description", createRequest.getDescription());
          map.putString("id", createRequest.getId());
          map.putString("subject", createRequest.getSubject());

          request.putMap("request", map);

          innerPromise.resolve(request);
        }

        @Override
        public void onError(ErrorResponse errorResponse) {
          Log.d(TAG, "onError: " + errorResponse.getReason());
          innerPromise.reject("onError", errorResponse.getReason());
        }
      } ;

      // Call the provider method
      provider.createRequest(zdRequest, callback);

    } catch (Exception e) {
      promise.reject("onException", e.getMessage());
    }
  }
}
