//
//  SpleeterSDK.h
//  SpleeterSDK
//
//  Created by user on 1/6/22.
//

#import <Foundation/Foundation.h>

//! Project version number for SpleeterSDK.
FOUNDATION_EXPORT double SpleeterSDKVersionNumber;

//! Project version string for SpleeterSDK.
FOUNDATION_EXPORT const unsigned char SpleeterSDKVersionString[];

// In this header, you should import all the public headers of your framework using statements like #import <SpleeterSDK/PublicHeader.h>

NS_ASSUME_NONNULL_BEGIN

@interface SpleeterSDK : NSObject

-(int) createSDK;
-(void) releaseSDK;
-(int) process: (NSString*) wavPath outPath: (NSString*) outPath;
-(int) stopProcess;
-(int) progress;
-(int) playSize;
-(int) playBuffer: (NSData*) buffer offset: (int) offset stemRatio: (float*) stemRatio;
-(int) saveAllStem: (NSString*) outPath;
-(int) saveOne: (NSString*) outPath stemRatio: (float*) stemRatio;

@end

NS_ASSUME_NONNULL_END

