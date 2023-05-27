//
//  PrimeView.swift
//  Counter
//
//  Created by Rick Clephas on 27/05/2023.
//  Copyright © 2023 orgName. All rights reserved.
//

import SwiftUI
import counter

struct PrimeView: View {
    
    var state: PrimeScreenState
    
    var body: some View {
        Text("\(state.number)")
          .font(.system(size: 36))
          .padding()
        if (state.isPrime) {
            Text("\(state.number) is a prime number!")
        } else {
            Text("\(state.number) is not a prime number.")
        }
        Button("Back") {
            state.eventSink(PrimeScreenEventPop())
        }.padding()
    }
}

struct PrimeView_Previews: PreviewProvider {
    static var previews: some View {
        PrimeView(state: PrimeScreenState(number: 0, isPrime: false, eventSink: { _ in }))
    }
}
