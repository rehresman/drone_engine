

Crone.run()
Crone.setEngine("Engine_ProjectName");

Engine_ProjectName.debug

//oscillators
Crone.engine.setCommand('freqMasterIn', 80);  //exp from 20 to 20000
Crone.engine.setCommand('grungeIn', 1); //lin from 0 to 1
//oscillator 1
Crone.engine.setCommand('freq1In', 10); // exp from 20 to 20000
Crone.engine.setCommand('spread1In', 14); // lin from 0 to 10 ?
//oscillator 2
Crone.engine.setCommand('freq2In', 1200); // exp from 20 to 20000
Crone.engine.setCommand('spread2In', 12); // lin from 0 to 10 ?

//mixer
Crone.engine.setCommand('level1In', 1); // log from 0 to 1
Crone.engine.setCommand('level2In', 1); // log from 0 to 1

//wavefolder
Crone.engine.setCommand('wavefolderAmtIn', 0.09); // custom polynomial from 0 to 1.  most of the knob range should be between 0.025 and 0.1

// filter
Crone.engine.setCommand('cutoffIn', 1300); // exp from 20 to 20000
Crone.engine.setCommand('resonanceIn', 0); // log from 0 to 0.95 ?

// lfo
Crone.engine.setCommand('ampLFOAmtIn', 1); // lin from 0 to 1
Crone.engine.setCommand('lfoRate1In', 0.3); // lin from 0.1 to 500 ?
Crone.engine.setCommand('lfoRate2In', 0.8); // lin from 0.1 to 500 ?
