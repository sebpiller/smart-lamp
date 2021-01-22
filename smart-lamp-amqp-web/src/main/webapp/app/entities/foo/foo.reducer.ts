import axios from 'axios';
import { ICrudGetAction, ICrudGetAllAction, ICrudPutAction, ICrudDeleteAction } from 'react-jhipster';

import { cleanEntity } from 'app/shared/util/entity-utils';
import { REQUEST, SUCCESS, FAILURE } from 'app/shared/reducers/action-type.util';

import { IFoo, defaultValue } from 'app/shared/model/foo.model';

export const ACTION_TYPES = {
  FETCH_FOO_LIST: 'foo/FETCH_FOO_LIST',
  FETCH_FOO: 'foo/FETCH_FOO',
  CREATE_FOO: 'foo/CREATE_FOO',
  UPDATE_FOO: 'foo/UPDATE_FOO',
  DELETE_FOO: 'foo/DELETE_FOO',
  RESET: 'foo/RESET',
};

const initialState = {
  loading: false,
  errorMessage: null,
  entities: [] as ReadonlyArray<IFoo>,
  entity: defaultValue,
  updating: false,
  totalItems: 0,
  updateSuccess: false,
};

export type FooState = Readonly<typeof initialState>;

// Reducer

export default (state: FooState = initialState, action): FooState => {
  switch (action.type) {
    case REQUEST(ACTION_TYPES.FETCH_FOO_LIST):
    case REQUEST(ACTION_TYPES.FETCH_FOO):
      return {
        ...state,
        errorMessage: null,
        updateSuccess: false,
        loading: true,
      };
    case REQUEST(ACTION_TYPES.CREATE_FOO):
    case REQUEST(ACTION_TYPES.UPDATE_FOO):
    case REQUEST(ACTION_TYPES.DELETE_FOO):
      return {
        ...state,
        errorMessage: null,
        updateSuccess: false,
        updating: true,
      };
    case FAILURE(ACTION_TYPES.FETCH_FOO_LIST):
    case FAILURE(ACTION_TYPES.FETCH_FOO):
    case FAILURE(ACTION_TYPES.CREATE_FOO):
    case FAILURE(ACTION_TYPES.UPDATE_FOO):
    case FAILURE(ACTION_TYPES.DELETE_FOO):
      return {
        ...state,
        loading: false,
        updating: false,
        updateSuccess: false,
        errorMessage: action.payload,
      };
    case SUCCESS(ACTION_TYPES.FETCH_FOO_LIST):
      return {
        ...state,
        loading: false,
        entities: action.payload.data,
        totalItems: parseInt(action.payload.headers['x-total-count'], 10),
      };
    case SUCCESS(ACTION_TYPES.FETCH_FOO):
      return {
        ...state,
        loading: false,
        entity: action.payload.data,
      };
    case SUCCESS(ACTION_TYPES.CREATE_FOO):
    case SUCCESS(ACTION_TYPES.UPDATE_FOO):
      return {
        ...state,
        updating: false,
        updateSuccess: true,
        entity: action.payload.data,
      };
    case SUCCESS(ACTION_TYPES.DELETE_FOO):
      return {
        ...state,
        updating: false,
        updateSuccess: true,
        entity: {},
      };
    case ACTION_TYPES.RESET:
      return {
        ...initialState,
      };
    default:
      return state;
  }
};

const apiUrl = 'api/foos';

// Actions

export const getEntities: ICrudGetAllAction<IFoo> = (page, size, sort) => {
  const requestUrl = `${apiUrl}${sort ? `?page=${page}&size=${size}&sort=${sort}` : ''}`;
  return {
    type: ACTION_TYPES.FETCH_FOO_LIST,
    payload: axios.get<IFoo>(`${requestUrl}${sort ? '&' : '?'}cacheBuster=${new Date().getTime()}`),
  };
};

export const getEntity: ICrudGetAction<IFoo> = id => {
  const requestUrl = `${apiUrl}/${id}`;
  return {
    type: ACTION_TYPES.FETCH_FOO,
    payload: axios.get<IFoo>(requestUrl),
  };
};

export const createEntity: ICrudPutAction<IFoo> = entity => async dispatch => {
  const result = await dispatch({
    type: ACTION_TYPES.CREATE_FOO,
    payload: axios.post(apiUrl, cleanEntity(entity)),
  });
  dispatch(getEntities());
  return result;
};

export const updateEntity: ICrudPutAction<IFoo> = entity => async dispatch => {
  const result = await dispatch({
    type: ACTION_TYPES.UPDATE_FOO,
    payload: axios.put(apiUrl, cleanEntity(entity)),
  });
  return result;
};

export const deleteEntity: ICrudDeleteAction<IFoo> = id => async dispatch => {
  const requestUrl = `${apiUrl}/${id}`;
  const result = await dispatch({
    type: ACTION_TYPES.DELETE_FOO,
    payload: axios.delete(requestUrl),
  });
  dispatch(getEntities());
  return result;
};

export const reset = () => ({
  type: ACTION_TYPES.RESET,
});
