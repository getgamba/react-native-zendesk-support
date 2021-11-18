//
//  RNZenDeskSupport.m
//
//  Created by Patrick O'Connor on 8/30/17.
//

// RN < 0.40 suppoert
#if __has_include(<React/RCTBridge.h>)
#import <React/RCTConvert.h>
#else
#import "RCTConvert.h"
#endif

#import "RNZenDeskSupport.h"
#import <SupportSDK/SupportSDK.h>
#import <ZendeskCoreSDK/ZendeskCoreSDK.h>
#import <SupportProvidersSDK/SupportProvidersSDK.h>

@implementation RNZenDeskSupport

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(initialize:(NSDictionary *)config){
    NSString *appId = [RCTConvert NSString:config[@"appId"]];
    NSString *zendeskUrl = [RCTConvert NSString:config[@"zendeskUrl"]];
    NSString *clientId = [RCTConvert NSString:config[@"clientId"]];
    
    [ZDKZendesk initializeWithAppId:appId clientId:clientId zendeskUrl:zendeskUrl];
    [ZDKSupport initializeWithZendesk:[ZDKZendesk instance]];

    id<ZDKObjCIdentity> userIdentity = [[ZDKObjCAnonymous alloc] initWithName:nil email:nil];
    [[ZDKZendesk instance] setIdentity:userIdentity];
}

RCT_EXPORT_METHOD(setupIdentity:(NSDictionary *)identity){
    dispatch_async(dispatch_get_main_queue(), ^{
        NSString *email = [RCTConvert NSString:identity[@"customerEmail"]];
        NSString *name = [RCTConvert NSString:identity[@"customerName"]];
        id<ZDKObjCIdentity> userIdentity = [[ZDKObjCAnonymous alloc] initWithName:name email:email];
        [[ZDKZendesk instance] setIdentity:userIdentity];
        
    });
}

RCT_EXPORT_METHOD(callSupport:(NSDictionary *)customFields) {
    dispatch_async(dispatch_get_main_queue(), ^{
        UIWindow *window=[UIApplication sharedApplication].keyWindow;
        NSMutableArray *fields = [[NSMutableArray alloc] init];
        for (NSString* key in customFields) {
            id value = [customFields objectForKey:key];
            [fields addObject: [[ZDKCustomField alloc] initWithFieldId:@(key.integerValue) value:value]];
        }
        ZDKRequestUiConfiguration * config = [ZDKRequestUiConfiguration new];
        config.customFields = fields;
        UIViewController *requestController = [ZDKRequestUi buildRequestUiWith:@[config]];
        
        UINavigationController *navController = [[UINavigationController alloc]initWithRootViewController:requestController];
        [window.rootViewController presentViewController:navController animated:YES completion:nil];
    });
}

@end
