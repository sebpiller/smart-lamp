import axios from 'axios';
import { defaultValue, IFoo } from 'app/shared/model/foo.model';
import { REQUEST } from 'app/shared/reducers/action-type.util';
import { ICrudGetAllAction, ICrudPutAction } from 'react-jhipster';
import { cleanEntity } from 'app/shared/util/entity-utils';

export const ACTION_TYPES = {
  SEND_COMMAND: 'amqp/SEND_COMMAND',
};

const initialState = {
  command: null,
};

export type AmqpState = Readonly<typeof initialState>;

// Reducer

export default (state: AmqpState = initialState, action): AmqpState => {
  switch (action.type) {
    case REQUEST(ACTION_TYPES.SEND_COMMAND):
      return state;
    default:
      return state;
  }
};

const apiUrl = 'amqp/command';

// Actions

export const sendCommand: ICrudPutAction<string> = entity => async dispatch => {
  const result = await dispatch({
    type: ACTION_TYPES.SEND_COMMAND,
    payload: axios.post(apiUrl, entity),
  });
  return result;
};
