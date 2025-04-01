// Get a mystery object with its sub-parts
export async function getMysteryObjectWithSubParts(id) {
    const response = await fetch(`/api/mysteryObjects/${id}`);
    if (!response.ok) {
        throw new Error(`Failed to fetch mystery object: ${response.statusText}`);
    }
    return await response.json();
}

// Get sub-parts of a mystery object
export async function getSubParts(parentId) {
    const response = await fetch(`/api/mysteryObjects/${parentId}/subParts`);
    if (!response.ok) {
        throw new Error(`Failed to fetch sub-parts: ${response.statusText}`);
    }
    return await response.json();
}

// Add a sub-part to a mystery object
export async function addSubPart(parentId, subPartData) {
    const response = await fetch(`/api/mysteryObjects/${parentId}/subParts`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(subPartData),
    });
    
    if (!response.ok) {
        throw new Error(`Failed to add sub-part: ${response.statusText}`);
    }
    return await response.json();
}

// Update a sub-part
export async function updateSubPart(parentId, subPartId, subPartData) {
    const response = await fetch(`/api/mysteryObjects/${parentId}/subParts/${subPartId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(subPartData),
    });
    
    if (!response.ok) {
        throw new Error(`Failed to update sub-part: ${response.statusText}`);
    }
    return await response.json();
}

// Remove a sub-part
export async function removeSubPart(parentId, subPartId) {
    const response = await fetch(`/api/mysteryObjects/${parentId}/subParts/${subPartId}`, {
        method: 'DELETE',
    });
    
    if (!response.ok) {
        throw new Error(`Failed to remove sub-part: ${response.statusText}`);
    }
    return true;
}
