//
//  CircuitSwiftUINavigator.h
//  Circuit
//
//  Created by Rick Clephas on 27/05/2023.
//

#ifndef CircuitSwiftUINavigator_h
#define CircuitSwiftUINavigator_h

#import <Foundation/Foundation.h>

@protocol CircuitSwiftUINavigator
- (void)goToScreen:(NSObject * _Nonnull)screen __attribute__((swift_name("goTo(screen:)")));
- (NSObject * _Nullable)pop __attribute__((swift_name("pop()")));
- (NSArray<NSObject *> * _Nonnull)resetRootNewRoot:(NSObject * _Nonnull)newRoot __attribute__((swift_name("resetRoot(newRoot:)")));
@end

#endif /* CircuitSwiftUINavigator_h */
