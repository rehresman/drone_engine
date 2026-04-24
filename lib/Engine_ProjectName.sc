Engine_ProjectName : CroneEngine {
	classvar <current;
	// server
	var s;
	// audio buses
	var <oscOut, <osc1Out, <osc2Out, <filterOut, <grungeOut, <vcaOut, <mixerOut, <lfoOut, <wavefolderIn, <wavefolderOut;
	// control buses
	var <freqMasterIn, <freq1In, freq2In, <cutoffIn, <resonanceIn, <grungeIn, <spread1In, <spread2In, <mixer1In, <mixer2In, <level1In;
	var <level2In, <ampLFOAmtIn, <lfoRate1In, <lfoRate2In, <wavefolderAmtIn;
	// groups
	var modGrp, audioGrp;
	// parameter map
	var paramMap;
	// synths
	var lfo, oscillator1, oscillator2, mixer1, wavefolder, filter1, saturator1, vca1, output1;
	// wavefolder
	var wavefolderScale,	minWavefolderInput,	maxWavefolderInput, wavefolderLUTSize, wavefolderTable, wavefolderFunc, wavefolderBuf;


	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {
		s = context.server;
		// init variables
		current = this;

		// control buses
		freqMasterIn = Bus.control(s,1);
		freq1In = Bus.control(s,1);
		freq2In = Bus.control(s,1);
		spread1In = Bus.control(s,1);
		spread2In = Bus.control(s,1);
		grungeIn = Bus.control(s,1);
		cutoffIn = Bus.control(s,1);
		resonanceIn = Bus.control(s,1);
		level1In = Bus.control(s,1);
		level2In = Bus.control(s,1);
		ampLFOAmtIn = Bus.control(s,1);
		lfoRate1In = Bus.control(s,1);
		lfoRate2In = Bus.control(s,1);
		wavefolderAmtIn = Bus.control(s,1);

		// audio buses
		osc1Out = Bus.audio(s,2);
		osc2Out = Bus.audio(s,2);
		oscOut = Bus.audio(s,2);
		filterOut = Bus.audio(s,2);
		grungeOut = Bus.audio(s,2);
		vcaOut = Bus.audio(s,2);
		mixer1In = Bus.audio(s,2);
		mixer2In = Bus.audio(s,2);
		mixerOut = Bus.audio(s, 2);
		lfoOut = Bus.audio(s, 2);
		wavefolderIn = Bus.audio(s,2);
		wavefolderOut = Bus.audio(s,2);

		// norns control of buses
		paramMap = IdentityDictionary[
			\freqMasterIn -> (bus: freqMasterIn),
			\freq1In -> (bus: freq1In),
			\freq2In -> (bus:freq2In),
			\spread1In -> (bus: spread1In),
			\spread2In -> (bus:spread2In),
			\grungeIn -> (bus: grungeIn),
			\cutoffIn -> (bus: cutoffIn),
			\resonanceIn -> (bus: resonanceIn),
			\level1In -> (bus: level1In),
			\level2In -> (bus: level2In),
			\ampLFOAmtIn -> (bus: ampLFOAmtIn),
			\wavefolderAmtIn -> (bus: wavefolderAmtIn),
			\lfoRate1In -> (bus: lfoRate1In),
			\lfoRate2In -> (bus: lfoRate2In)
		];

		// initialize control buses
		freqMasterIn.set(85);
		freq1In.set(0);
		freq2In.set(0);
		spread1In.set(1);
		spread2In.set(0);
		cutoffIn.set(20000);
		resonanceIn.set(0);
		grungeIn.set(0);
		level1In.set(1);
		level2In.set(1);
		ampLFOAmtIn.set(0);
		wavefolderAmtIn.set(0);
		lfoRate1In.set(1);
		lfoRate2In.set(1.2);
		s.sync;

		// Build Wavefolder LUT
		//////////////////////////////////////////////////////////////////////

		wavefolderScale = 8;
		minWavefolderInput = -8.0;
		maxWavefolderInput = 8.0;
		wavefolderLUTSize = 16384;

		//wavefolder based on DAFx-17
		//https://www.dafx17.eca.ed.ac.uk/papers/DAFx17_paper_82.pdf
		wavefolderFunc = { |x, smoothness = 0.1|
			var s = x.sign;
			var fastSmoothActivation = { |input, threshold|
				var absx = input.abs;
				var delta = absx - threshold;
				((delta > 0).asInteger * (delta < smoothness).asInteger* (delta / smoothness))
				+ ((delta >= smoothness).asInteger);
			};
			var fold1 = fastSmoothActivation.(x, 0.6) * (0.8333 * x - (0.5000 * s));
			var fold2 = fastSmoothActivation.(x, 2.9940) * (0.3768 * x - (1.1281 * s));
			var fold3 = fastSmoothActivation.(x, 5.4600) * (0.2829 * x - (1.5446 * s));
			var fold4 = fastSmoothActivation.(x, 1.8000) * (0.5743 * x - (1.0338 * s));
			var fold5 = fastSmoothActivation.(x, 4.0800) * (0.2673 * x - (1.0907 * s));
			var direct = x;
			var output = (
				((-12.000) * fold1) + ((-27.777) * fold2) + ((-21.428) * fold3) +
				(17.647 * fold4) + (36.363 * fold5) + (5.000 * direct)
			) / 40;
			output;
		};

		wavefolderTable = Signal.fill((wavefolderLUTSize / 2) + 1, {
			|i|
			var x = i.linlin(0.0,
				wavefolderLUTSize / 2,
				minWavefolderInput,
				maxWavefolderInput);
			var y = wavefolderFunc.(x);
			y;
		});

		// load buffers
		wavefolderBuf = Buffer.alloc(s, wavefolderLUTSize);
		s.sync;
		wavefolderBuf.loadCollection(wavefolderTable.asWavetableNoWrap);
		s.sync;

		// Create SynthDefs

		SynthDef(\oscillator, {|freqMaster, freqIndividual, spread, grungeAmt, crossInBus, outBus, fmOutBus, fmInBus, flip, lfoAmt, ampInBus|
			var sig, fm, crossAmt, freq, amp, instability;
			fm = InFeedback.ar(fmInBus, 2);
			amp = In.ar(ampInBus, 2);
			instability = BrownNoise.kr(0.001);

			amp = 1 + (lfoAmt * (amp/2 - DC.ar(0.5)));
			crossAmt = grungeAmt;
			fm = fm * crossAmt;
			freq = Lag.kr((freqMaster+freqIndividual)*(1+instability),0.18);
			freq = Fold.ar(freq* exp(fm), 20,20000);
			sig = SinOsc.ar([freq+(spread*flip),freq+(spread*(1-flip))]);
			sig = sig * amp;

			Out.ar(fmOutBus,sig);
			Out.ar(outBus, sig);
		}).add;

		SynthDef(\mixer, {|in1Bus, in2Bus, in1Level, in2Level, outBus|
			var in1, in2, sig;

			in1 = In.ar(in1Bus, 2);
			in2 = In.ar(in2Bus, 2);

			sig = (in1 * in1Level) + (in2 * in2Level);

			Out.ar(outBus, sig);
		}).add;

		SynthDef(\filter, {|cutoff, res, outBus, satAmt, inBus|
			var sig;

			satAmt = satAmt /4;

			sig = In.ar(inBus, 2);
			sig = sig * (1 + satAmt);
			sig = sig.tanh;
			sig = RLPF.ar(sig, Lag.kr(cutoff,0.18), 1-res);
			sig = (sig - DC.ar(0.6)).tanh + DC.ar(0.535);
			sig = sig / (1+satAmt);

			Out.ar(outBus, sig);
		}).add;

		SynthDef(\grunge, {|inBus, outBus|
			var sig, grungeAmt;

			grungeAmt = In.kr(grungeIn, 1);
			sig = In.ar(inBus, 2);

			Out.ar(outBus, sig);
		}).add;

		SynthDef(\stereoVCA, {|outBus|
			var sig;

			sig = In.ar(grungeOut, 2);

			Out.ar(outBus, sig);
		}).add;

		SynthDef(\dualLFO, {|rate1, rate2, outBus|
			var sig, instability;

			instability = BrownNoise.kr(0.002);
			sig = [VarSaw.ar(rate1*(1+instability)), VarSaw.ar(rate2*(1-instability))];
			Out.ar(outBus, sig);
		}).add;

		SynthDef(\wavefolder, {|preGain=1, inBus, outBus|
			var sig, index;

			sig = In.ar(inBus, 2) * (0.15+(preGain*1.82));
			sig= Shaper.ar(wavefolderBuf, sig / wavefolderScale);

			Out.ar(outBus, sig *5);
		}).add;


		SynthDef(\output, {|inBus|
			Out.ar(0, In.ar(inBus,2));
		}).add;

		s.sync;
		// Create Groups
		modGrp = Group.head(s);
		s.sync;
		audioGrp = Group.tail(modGrp);
		s.sync;
		// Create Synths
		lfo = Synth(\dualLFO, [rate1: lfoRate1In.asMap, rate2: lfoRate2In.asMap, outBus: lfoOut], modGrp);
		oscillator1 = Synth(\oscillator, [freqMaster: freqMasterIn.asMap,
			freqIndividual: freq1In.asMap,
			spread: spread1In.asMap,
			grungeAmt: grungeIn.asMap,
			outBus: mixer1In,
			fmOutBus: osc1Out,
			fmInBus: osc2Out,
			flip: 0,
			lfoAmt: ampLFOAmtIn.asMap,
			ampInBus: lfoOut
		], audioGrp);
		oscillator2 = Synth(\oscillator, [freqMaster: freqMasterIn.asMap,
			freqIndividual: freq2In.asMap,
			spread: spread2In.asMap,
			grungeAmt: grungeIn.asMap,
			outBus: mixer2In,
			fmOutBus: osc2Out,
			fmInBus: osc1Out,
			flip: 1,
			lfoAmt: ampLFOAmtIn.asMap,
			ampInBus: lfoOut
		], audioGrp);
		s.sync;
		mixer1 = Synth.tail(audioGrp, \mixer, [in1Bus: mixer1In, in2Bus: mixer2In, in1Level: level1In.asMap, in2Level: level2In.asMap, outBus: mixerOut]);
		s.sync;
		wavefolder = Synth.tail(audioGrp, \wavefolder, [inBus: mixerOut, outBus: wavefolderOut, preGain: wavefolderAmtIn.asMap]);
		s.sync;
		filter1 = Synth.tail(audioGrp, \filter, [cutoff: cutoffIn.asMap, res: resonanceIn.asMap, inBus: wavefolderOut, outBus: filterOut, satAmt: grungeIn.asMap]);
		s.sync;
		saturator1 = Synth.tail(audioGrp, \grunge, [inBus: filterOut, outBus: grungeOut]);
		s.sync;
		vca1 = Synth.tail(audioGrp, \stereoVCA, [outBus: vcaOut]);
		s.sync;
		output1 = Synth.tail(audioGrp, \output, [inBus: vcaOut]);
		// Create MIDI Handlers (todo...?)

		// allow lua interface through bus names
		paramMap.keys.do { |name|
			this.addCommand(name, "f", { |msg|
				var param, value;
				param = paramMap[name];
				value = msg[1];

				if (param.notNil) {
					param[\bus].set(value);
				};
			});
		};

	}

	*debug {
		("oscOut bus index:" ++ current.oscOut.index).postln;
		("osc1Out bus index:" ++ current.osc1Out.index).postln;
		("osc2Out bus index:" ++ current.osc2Out.index).postln;
		("filterOut bus index:" ++ current.filterOut.index).postln;
		("grungeOut bus index:" ++ current.grungeOut.index).postln;
		("vcaOut bus index:" ++ current.vcaOut.index).postln;
		("freqMasterIn bus index:" ++ current.freqMasterIn.index).postln;
		("cutoffIn bus index:" ++ current.cutoffIn.index).postln;
		("grungeIn bus index:" ++ current.grungeIn.index).postln;
		("lfoOut bus index:" ++ current.lfoOut.index).postln;
		("ampLFOAmtIn bus index:" ++ current.ampLFOAmtIn.index).postln;
		("mixerOut bus index:" ++ current.mixerOut.index).postln;
		("wavefolderOut bus index:" ++ current.wavefolderOut.index).postln;
	}

	free {
		modGrp.free;
		audioGrp.free;
	}
}