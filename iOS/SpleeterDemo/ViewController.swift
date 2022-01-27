//
//  ViewController.swift
//  SpleeterDemo
//
//  Created by user on 1/6/22.
//

import UIKit
import AVFoundation

class ViewController: UIViewController {
    
    @IBOutlet weak var progress: UILabel!
    @IBOutlet weak var lblOut: UILabel!
    @IBOutlet weak var btnProcess: UIButton!
    @IBOutlet weak var sldVocal: UISlider!
    @IBOutlet weak var sldDrum: UISlider!
    @IBOutlet weak var sldBass: UISlider!
    @IBOutlet weak var sldOther: UISlider!
    @IBOutlet weak var sldPiano: UISlider!
    
    var spleeterSDK: SpleeterSDK!
    var stemRatio: [Float] = [1.0, 1.0, 1.0, 1.0, 1.0] //vocal, pinao, drum, bass, other
    var player:AVAudioPlayer?

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
               
        spleeterSDK = SpleeterSDK();
        let ret = spleeterSDK.createSDK()
        print("create SDK: ", ret);
    }

    @IBAction func ProcessClicked(_ sender: Any) {
        btnProcess.isEnabled = false
        
        self.stemRatio[0] = self.sldVocal.value
        self.stemRatio[1] = self.sldPiano.value
        self.stemRatio[2] = self.sldDrum.value
        self.stemRatio[3] = self.sldBass.value
        self.stemRatio[4] = self.sldOther.value
        
        let wavPath = Bundle.main.path(forResource: "_input.wav", ofType: nil)
        
        let manager = FileManager.default
        guard let url = manager.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return
        }
        print(url.path)
        
        self.lblOut.text = url.path
        spleeterSDK.stopProcess()
        let ret = spleeterSDK.process(wavPath!, outPath: url.path)
        if(ret == 0) {
            let queue = DispatchQueue(label: "process-queue")
            queue.async {
                while(true) {
                    let progress = self.spleeterSDK.progress()
                    DispatchQueue.main.async {
                        self.progress.text = String(progress) + "%"
                    }
                    usleep(1000 * 1000);
                    
                    if(progress == 100) {
                        break
                    }
                }
               
                self.spleeterSDK.saveOne(url.path + "/record.wav", stemRatio: UnsafeMutablePointer<Float32>(mutating: self.stemRatio))
                
                DispatchQueue.main.async {
                    self.btnProcess.isEnabled = true
                    
                    do {
                        try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
                        try AVAudioSession.sharedInstance().setActive(true)
                        
                        self.player = try AVAudioPlayer(contentsOf: URL(fileURLWithPath: url.path + "/record.wav"))
                        guard let player = self.player else {
                            return
                        }

                        player.play()
                    } catch let error {
                        print(error.localizedDescription)
                    }
                }
            }
        }
    }
}

