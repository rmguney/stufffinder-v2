import { writable } from 'svelte/store';

const storedUser = typeof localStorage !== 'undefined' ? localStorage.getItem('currentUser') : null;
export const activeUser = writable(storedUser);

// Subscribe to changes and update localStorage
if (typeof localStorage !== 'undefined') {
    activeUser.subscribe(value => {
        if (value) {
            localStorage.setItem('currentUser', value);
        } else {
            localStorage.removeItem('currentUser');
        }
    });
}
