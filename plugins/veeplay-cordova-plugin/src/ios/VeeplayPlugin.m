//
//  VeeplayPlugin.m
//  VeeplayPlugin
//
//  Created by Ovidiu Nitan on 14/04/16.
//
//

#import "VeeplayPlugin.h"

@implementation VeeplayPlugin {
    NSString *_eventDelegateCallbackId;
    BOOL _needsFullscreen;
}

- (void) domElementPosition {
    UIWebView *webView = (UIWebView*)self.webView;
    [webView stringByEvaluatingJavaScriptFromString:@"window.veeplay.setBounds();"];
}

- (void) bindInternalBridge:(CDVInvokedUrlCommand *)command {
}

- (void) setBounds:(CDVInvokedUrlCommand *)command {
    if ([[APSMediaPlayer sharedInstance] isFullscreen]) return;
    CGRect playerFrame = CGRectMake([command.arguments[0] floatValue], [command.arguments[1] floatValue], [command.arguments[2] floatValue], [command.arguments[3] floatValue]);
    [APSMediaPlayer sharedInstance].view.frame = playerFrame;
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
}

- (void) appStarted:(CDVInvokedUrlCommand *)command {
}

- (void) configureCastSettings:(CDVInvokedUrlCommand *)command {
}

- (void) subscribeToEventNotifications {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(eventReceived:) name:APSMediaPlayerTrackedEventNotification object:nil];
}

- (void) eventReceived:(NSNotification*)notification {
    if (_needsFullscreen && [notification.userInfo[kAPSMediaPlayerEventType] isEqualToString:@"start"]) {
        [[APSMediaPlayer sharedInstance] enterFullscreen];
        _needsFullscreen = NO;
    } else if (_needsFullscreen && [notification.userInfo[kAPSMediaPlayerEventType] isEqualToString:@"finish"]) {
        UIWebView *webView = (UIWebView*)self.webView;
        [webView stringByEvaluatingJavaScriptFromString:@"window.veeplay.stopMonitoring();"];
    }
    if (_eventDelegateCallbackId) {
        UIWebView *webView = (UIWebView*)self.webView;
        NSError *error;
        NSMutableDictionary *newDict = [NSMutableDictionary dictionaryWithDictionary:notification.userInfo];
        [newDict removeObjectForKey:@"event.source"];
        NSData *jsonData = [NSJSONSerialization dataWithJSONObject:newDict
                                                           options:0
                                                             error:&error];
        [webView stringByEvaluatingJavaScriptFromString:[NSString stringWithFormat:@"window.veeplay.onTrackingEvent(%@);", [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding]]];
    }
}

- (void) play:(CDVInvokedUrlCommand*)command {
    [self subscribeToEventNotifications];
    UIWebView *webView = (UIWebView*)self.webView;
    [webView.scrollView addSubview:[APSMediaPlayer sharedInstance].view];
    CGRect playerFrame = CGRectMake([command.arguments[1] floatValue], [command.arguments[2] floatValue], [command.arguments[3] floatValue], [command.arguments[4] floatValue]);
    [APSMediaPlayer sharedInstance].view.frame = playerFrame;
    
    APSMediaBuilder *builder = [[APSMediaBuilder alloc] init];
    [builder addPlugin:[[APSVASTMediaBuilderPlugin alloc] init]];
    
    NSError *error = nil;
    id object = [NSJSONSerialization
                 JSONObjectWithData:[command.arguments[0] dataUsingEncoding:NSUTF8StringEncoding]
                 options:0
                 error:&error];
    if (error) {
        [builder configureFromURL:[NSURL URLWithString:command.arguments[0]] onComplete:^ {
            [builder requestMediaUnitsWithCompletionBlock:^(NSArray *units) {
                [[APSMediaPlayer sharedInstance] playMediaUnits:units];
            }];
        }];
    } else if ([object isKindOfClass:[NSDictionary class]]) {
        [builder configureFromDictionary:object];
        [builder requestMediaUnitsWithCompletionBlock:^(NSArray *units) {
            [[APSMediaPlayer sharedInstance] playMediaUnits:units];
        }];
    }
    
    if ([command.arguments[5] boolValue]) {
        _needsFullscreen = YES;
    }

    _eventDelegateCallbackId = command.callbackId;
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
}

- (void) stop:(CDVInvokedUrlCommand*)command {
    [[APSMediaPlayer sharedInstance] stop];
    [[APSMediaPlayer sharedInstance].view removeFromSuperview];
}

- (void) pause:(CDVInvokedUrlCommand*)command {
    [[APSMediaPlayer sharedInstance] pause];
}

- (void) resume:(CDVInvokedUrlCommand*)command {
    [[APSMediaPlayer sharedInstance] play];
}

- (void) duration:(CDVInvokedUrlCommand*)command {
    if ([APSMediaPlayer sharedInstance].currentUnit) {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:[APSMediaPlayer sharedInstance].duration] callbackId:command.callbackId];
    } else {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"No current video playing"] callbackId:command.callbackId];
    }
}

- (void) bufferedTime:(CDVInvokedUrlCommand*)command {
    if ([APSMediaPlayer sharedInstance].currentUnit) {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:[APSMediaPlayer sharedInstance].playableDuration] callbackId:command.callbackId];
    } else {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"No current video playing"] callbackId:command.callbackId];
    }
}

- (void) toggleFullscreen:(CDVInvokedUrlCommand*)command {
    if ([APSMediaPlayer sharedInstance].currentUnit) {
        [[APSMediaPlayer sharedInstance] toggleFullscreen];
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
    } else {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"No current video playing"] callbackId:command.callbackId];
    }
}

- (void) mute:(CDVInvokedUrlCommand*)command {
    [[APSMediaPlayer sharedInstance] setMute:YES];
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
}

- (void) unMute:(CDVInvokedUrlCommand*)command {
    [[APSMediaPlayer sharedInstance] setMute:NO];
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
}

- (void) skip:(CDVInvokedUrlCommand*)command {
    if ([APSMediaPlayer sharedInstance].currentUnit) {
        [[APSMediaPlayer sharedInstance] skip];
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
    } else {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"No current video playing"] callbackId:command.callbackId];
    }
}

- (void) back:(CDVInvokedUrlCommand*)command {
    if ([APSMediaPlayer sharedInstance].currentUnit) {
        [[APSMediaPlayer sharedInstance] previous];
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
    } else {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"No current video playing"] callbackId:command.callbackId];
    }
}

- (void) isPlaying:(CDVInvokedUrlCommand*)command {
    if ([APSMediaPlayer sharedInstance].currentUnit) {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:[APSMediaPlayer sharedInstance].isProcessing] callbackId:command.callbackId];
    } else {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"No current video playing"] callbackId:command.callbackId];
    }
}

@end
