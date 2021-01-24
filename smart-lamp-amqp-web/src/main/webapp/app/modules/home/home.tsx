import './home.scss';

import React, {Component, useState,} from "react";
import {Row, Col} from 'reactstrap';
import {Translate} from 'react-jhipster';
import {Checkbox, MenuItem, Select, Slider} from '@material-ui/core';
import {connect} from 'react-redux';
import axios from "axios";
import Switch from "react-switch";
import {HuePicker} from 'react-color';

export type IHomeProp = StateProps;

export const Home = (props: IHomeProp) => {
  const [checked, setChecked] = useState(false);
  const [scene, setScene] = useState('0');
  const [brightness, setBrightness] = useState(100);
  const [temperature, setTemperature] = useState(4000);
  const [color, setColor] = useState("#ffffff");
  const [whiteChecked, setWhiteChecked] = useState(true);
  const [lastCommand, setLastCommand] = useState("none");

  const sendCommand = command => {
    // eslint-disable-next-line no-console
    console.log("command received: " + command + " " + lastCommand);

    if (lastCommand !== command) { // prevent multiple message with same command
      setLastCommand(command)
      axios
        .post("amqp/command", command)
        .then(e => {
          // eslint-disable-next-line no-console
          console.log("command accepted: " + command);
        })
    }
  }

  const handleChange = nextChecked => {
    setChecked(nextChecked)
    sendCommand(nextChecked ? "on" : "off")
  }

  const sceneChanged = (evt, state) => {
    setScene(state.props.value)
    setChecked(state.props.value !== 0)
    sendCommand("scene=" + state.props.value)
  }

  const brightnessChanged = (evt, value) => {
    setBrightness(value)
    sendCommand("brightness=" + value)
  }

  const temperatureChanged = (evt, value) => {
    setTemperature(value)
    sendCommand("temperature=" + value)
  }

  const applyColor = (f) => {
    if (f) {
      sendCommand("color=ffffff")
    } else {
      sendCommand("color=" + color.substr(1))
    }
  }

  const whiteCheckedChangeHandler = (evt, value) => {
    setWhiteChecked(value)
    applyColor(value)
  }

  const colorChangeCompleteHandler = (value, evt) => {
    setColor(value.hex)
    applyColor(whiteChecked)
  }

  const valuetext = t => t;

  return (
    <Row>
      <Col md="9">
        <h2><Translate contentKey="home.title">Smart Lamp management</Translate></h2>
        <p className="lead">
          <Translate contentKey="home.subtitle">Pilotez votre Lamp F en envoyant des messages AMQP.</Translate>
        </p>

        <Row>
          <Col md="3">
            <label>
              <span>Lamp F: {checked ? "on" : "off"}</span>
            </label>
          </Col>
          <Col md="6">
            <Switch
              onChange={handleChange}
              checked={checked}
              className="react-switch"
            />
          </Col>
        </Row>

        <Row>
          <Col md="3">
            <label>
              <span>Scene: {scene}</span>
            </label>
          </Col>
          <Col md="6">
            <Select
              onChange={sceneChanged}
            >
              <MenuItem value={0}>SHUTDOWN</MenuItem>
              <MenuItem value={6}>READING</MenuItem>
              <MenuItem value={4}>CANDLE LIGHT</MenuItem>
              <MenuItem value={3}>SHINY</MenuItem>
              <MenuItem value={1}>WELCOME</MenuItem>
              <MenuItem value={5}>INDIRECT</MenuItem>
              <MenuItem value={2}>HIGHLIGHTS</MenuItem>
              <MenuItem value={7}>BRIGHT</MenuItem>
            </Select>
          </Col>
        </Row>


        <Row>
          <Col md="3">
            <label>
              <span>Brillance de la lampe:</span>
              <br/>
              <span>{
                brightness < 50 ?
                  "TOP: " + (brightness * 2) + "%" :
                  "TOP: 100%, BOTTOM: " + ((brightness - 50) * 2) + "%"
              }</span>
            </label>
          </Col>

          <Col md="6">
            <Slider
              getAriaValueText={valuetext}
              step={1}
              marks
              min={0}
              max={100}
              aria-labelledby="discrete-slider"
              valueLabelDisplay="auto"
              value={brightness}
              onChange={brightnessChanged}
            />
          </Col>
        </Row>


        <Row>
          <Col md="3">
            <label>
              <span>Température de la lampe: {temperature}K</span>
            </label>
          </Col>

          <Col md="6">
            <Slider
              getAriaValueText={valuetext}
              step={50}
              marks
              min={2700}
              max={4000}
              aria-labelledby="discrete-slider"
              valueLabelDisplay="auto"
              value={temperature}
              onChange={temperatureChanged}
            />
          </Col>
        </Row>

        <Row>
          <Col md="3">
            <label>
              <span>Couleur de la lampe: {color}</span>
            </label>
          </Col>

          <Col md="6">
            <HuePicker
              disabled={whiteChecked}
              color={color}
              onChangeComplete={colorChangeCompleteHandler}/>

            <Checkbox
              checked={whiteChecked}
              onChange={whiteCheckedChangeHandler}/>
            white (#ffffff)
          </Col>
        </Row>
      </Col>
    </Row>
  );

}

const mapStateToProps = storeState => ({
  account: storeState.authentication.account,
  isAuthenticated: storeState.authentication.isAuthenticated,
});

type StateProps = ReturnType<typeof mapStateToProps>;

export default connect(mapStateToProps)(Home);
